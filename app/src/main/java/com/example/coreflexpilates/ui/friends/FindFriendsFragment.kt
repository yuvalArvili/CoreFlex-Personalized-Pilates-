package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.FriendRequest
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindFriendsFragment : Fragment(R.layout.fragment_find_friends) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: FindFriendsAdapter
    private val users = mutableListOf<User>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFriends)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FindFriendsAdapter(users) { user -> sendFriendRequest(user) }
        recyclerView.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                users.clear()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    if (user.uid != currentUserId) {
                        users.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendFriendRequest(user: User) {
        val currentUserId = auth.currentUser?.uid ?: return

        // בדיקה אם קיימת כבר בקשה
        firestore.collection("friend_requests")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiverId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "Request already sent", Toast.LENGTH_SHORT).show()
                } else {
                    val request = FriendRequest(
                        senderId = currentUserId,
                        receiverId = user.uid,
                        status = "pending"
                    )
                    firestore.collection("friend_requests")
                        .add(request)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Friend request sent", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check requests", Toast.LENGTH_SHORT).show()
            }
    }
}
