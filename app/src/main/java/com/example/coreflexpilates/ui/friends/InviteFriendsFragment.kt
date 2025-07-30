package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentInviteFriendsBinding
import com.example.coreflexpilates.model.Friendship
import com.example.coreflexpilates.model.LessonInvitation
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        // קבל את lessonId שהועבר דרך Bundle
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
        adapter = InviteFriendsAdapter(friends) { friend ->
            sendInvitation(friend.uid)
        }

        binding.recyclerViewInviteFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewInviteFriends.adapter = adapter

        loadFriends()
    }

    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("friendships")
            .whereEqualTo("user1Id", currentUserId)
            .get()
            .addOnSuccessListener { result1 ->
                val friendIds = result1.map { it.getString("user2Id") ?: "" }.toMutableSet()

                db.collection("friendships")
                    .whereEqualTo("user2Id", currentUserId)
                    .get()
                    .addOnSuccessListener { result2 ->
                        friendIds.addAll(result2.map { it.getString("user1Id") ?: "" })

                        if (friendIds.isEmpty()) return@addOnSuccessListener

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
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
