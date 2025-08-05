package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.R
import com.example.coreflexpilates.databinding.FragmentFriendsListBinding
import com.example.coreflexpilates.model.FriendRequest
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class FriendsListFragment : Fragment() {

    private var _binding: FragmentFriendsListBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val allUsers = mutableListOf<User>()
    private val friends = mutableListOf<User>()
    private val friendIds = mutableSetOf<String>()
    private val requestedUserIds = mutableSetOf<String>()

    private lateinit var friendsAdapter: FriendsListAdapter
    private lateinit var findFriendsAdapter: FindFriendsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
        friendsAdapter = FriendsListAdapter(friends) { user ->
            // Navigate to friend's lessons on friend item click
            val bundle = Bundle().apply {
                putString("friendId", user.uid)
                putString("friendName", user.name)
            }
            findNavController().navigate(R.id.FriendLessonsFragment, bundle)
        }
        binding.recyclerViewFriends.adapter = friendsAdapter

        // Initialize search results adapter with empty list and current friend IDs
        findFriendsAdapter = FindFriendsAdapter(emptyList(), friendIds) { user ->
            sendFriendRequest(user)
        }

        // Setup search listener for filtering users dynamically
        binding.searchViewFriends.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // If search text is empty, show the friends list
                    binding.recyclerViewFriends.adapter = friendsAdapter
                    friendsAdapter.notifyDataSetChanged()
                    binding.textNoFriends.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    // Filter all users by name or email
                    val filtered = allUsers.filter {
                        (it.name.contains(newText, ignoreCase = true) ||
                                it.email.contains(newText, ignoreCase = true)) &&
                                it.uid != auth.currentUser?.uid
                    }

                    // Update the search adapter's list
                    findFriendsAdapter.updateList(filtered)
                    binding.recyclerViewFriends.adapter = findFriendsAdapter
                    binding.textNoFriends.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                }
                return true
            }
        })

        // Load pending friend requests
        loadRequestedUserIds()
    }

    // Load users that pending
    private fun loadRequestedUserIds() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("friend_requests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                requestedUserIds.clear()
                for (doc in snapshot.documents) {
                    val receiverId = doc.getString("receiverId")
                    if (receiverId != null) {
                        requestedUserIds.add(receiverId)
                    }
                }
                loadFriends()
            }
    }

    // Load friend IDs of current user
    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return
        friendIds.clear()

        // Query friendships where current user is user1
        firestore.collection("friendships")
            .whereEqualTo("user1Id", currentUserId)
            .get()
            .addOnSuccessListener { result1 ->
                result1.forEach { doc -> friendIds.add(doc.getString("user2Id") ?: "") }

                // Query friendships where current user is user2
                firestore.collection("friendships")
                    .whereEqualTo("user2Id", currentUserId)
                    .get()
                    .addOnSuccessListener { result2 ->
                        result2.forEach { doc -> friendIds.add(doc.getString("user1Id") ?: "") }

                        loadAllUsers(currentUserId)
                    }
            }
    }

    // Load all users
    private fun loadAllUsers(currentUserId: String) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                allUsers.clear()
                friends.clear()

                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    if (user.uid != currentUserId) {
                        allUsers.add(user)
                        if (friendIds.contains(user.uid)) {
                            friends.add(user)
                        }
                    }
                }

                // Notify the friends adapter that data has changed
                friendsAdapter.notifyDataSetChanged()
            }
    }

    // Send a friend request
    private fun sendFriendRequest(user: User) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("friend_requests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiverId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val request = FriendRequest(
                        senderId = currentUserId,
                        receiverId = user.uid,
                        status = "pending"
                    )
                    firestore.collection("friend_requests")
                        .add(request)
                        .addOnSuccessListener {
                            sendFriendRequestNotification(user.uid)
                        }
                }
            }
    }

    // Send a push notification to the user receiving the friend request
    private fun sendFriendRequestNotification(receiverId: String) {
        if (receiverId.isBlank()) return

        firestore.collection("users").document(receiverId)
            .get()
            .addOnSuccessListener { doc ->
                val token = doc.getString("fcmToken")
                if (token.isNullOrBlank()) return@addOnSuccessListener

                val currentUserName = auth.currentUser?.displayName ?: "Someone"
                val data = mapOf(
                    "title" to "New Friend Request",
                    "body" to "$currentUserName sent you a friend request"
                )

                val message = RemoteMessage.Builder("$token@fcm.googleapis.com")
                    .setMessageId(UUID.randomUUID().toString())
                    .setData(data)
                    .build()

                FirebaseMessaging.getInstance().send(message)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
