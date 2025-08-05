package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentInviteFriendsBinding
import com.example.coreflexpilates.model.LessonInvitation
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class InviteFriendsFragment : Fragment() {

    private var _binding: FragmentInviteFriendsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val friends = mutableListOf<User>()
    private lateinit var adapter: InviteFriendsAdapter

    private lateinit var lessonId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInviteFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup toolbar back navigation
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        adapter = InviteFriendsAdapter(friends) { friend ->
            sendInvitation(friend.uid)
        }

        // Setup RecyclerView for displaying friends list
        binding.recyclerViewInviteFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewInviteFriends.adapter = adapter

        // Load the current user's friends from Firestore
        loadFriends()
    }

    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Query friendships where current user is user1
        db.collection("friendships")
            .whereEqualTo("user1Id", currentUserId)
            .get()
            .addOnSuccessListener { result1 ->
                val friendIds = result1.map { it.getString("user2Id") ?: "" }.toMutableSet()

                // Query friendships where current user is user2
                db.collection("friendships")
                    .whereEqualTo("user2Id", currentUserId)
                    .get()
                    .addOnSuccessListener { result2 ->
                        friendIds.addAll(result2.map { it.getString("user1Id") ?: "" })

                        if (friendIds.isEmpty()) return@addOnSuccessListener

                        // Get User documents for all friend IDs
                        db.collection("users")
                            .whereIn("uid", friendIds.toList())
                            .get()
                            .addOnSuccessListener { usersSnapshot ->
                                friends.clear()
                                for (doc in usersSnapshot.documents) {
                                    friends.add(doc.toObject(User::class.java)!!)
                                }
                                adapter.notifyDataSetChanged()
                            }
                    }
            }
    }

    // Send lesson invitation
    private fun sendInvitation(receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return

        val invitation = LessonInvitation(
            senderId = senderId,
            receiverId = receiverId,
            lessonId = lessonId
        )

        db.collection("lesson_invitations")
            .add(invitation)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Invitation sent", Toast.LENGTH_SHORT).show()
                // After sending invitation, send push notification
                sendInvitationNotification(receiverId)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    // Send push notification to invited friend
    private fun sendInvitationNotification(receiverId: String) {
        db.collection("users").document(receiverId)
            .get()
            .addOnSuccessListener { doc ->
                val token = doc.getString("fcmToken")
                if (token.isNullOrBlank()) {
                    Log.e("PushNotification", "No FCM token for user $receiverId")
                    return@addOnSuccessListener
                }

                val senderName = auth.currentUser?.displayName ?: "Someone"
                val data = mapOf(
                    "title" to "New Lesson Invitation",
                    "body" to "$senderName invited you to a lesson"
                )

                val message = RemoteMessage.Builder("$token@fcm.googleapis.com")
                    .setMessageId(UUID.randomUUID().toString())
                    .setData(data)
                    .build()

                try {
                    FirebaseMessaging.getInstance().send(message)
                    Log.d("PushNotification", "Push notification sent to $receiverId with token $token")
                } catch (e: Exception) {
                    Log.e("PushNotification", "Failed to send push notification", e)
                }
            }
            .addOnFailureListener {
                Log.e("PushNotification", "Failed to fetch user document for $receiverId", it)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

