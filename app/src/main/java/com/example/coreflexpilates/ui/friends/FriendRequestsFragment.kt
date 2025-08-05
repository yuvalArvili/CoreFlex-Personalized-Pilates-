package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentFriendRequestsBinding
import com.example.coreflexpilates.model.FriendRequest
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requests = mutableListOf<FriendRequest>()
    private val userNames = mutableMapOf<String, String>()
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar back button
        val toolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        adapter = FriendRequestsAdapter(requests, userNames,
            onAcceptClick = { request -> acceptRequest(request) },
            onDeclineClick = { request -> declineRequest(request) }
        )
        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRequests.adapter = adapter

        // Load pending friend requests on startup
        loadPendingRequests()
    }

    // Load all users
    private fun loadUserNames(onComplete: () -> Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                userNames.clear()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    Log.d("FriendRequests", "Loading user: ${user.uid} -> ${user.name}")
                    userNames[user.uid] = user.name
                }
                onComplete()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user names", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }


    // Load friend requests
    private fun loadPendingRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("friend_requests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                requests.clear()
                for (doc in snapshot.documents) {
                    val req = doc.toObject(FriendRequest::class.java)
                    if (req != null) {
                        requests.add(req.copy(id = doc.id))  // Add Firestore doc id for reference
                    }
                }
                Log.d("FriendRequests1", "Loaded ${requests.size} pending requests")

                // After loading requests, load user names and update adapter
                loadUserNames {
                    adapter.updateRequests(requests)
                    binding.textNoRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load friend requests", Toast.LENGTH_SHORT).show()
            }
    }

    // Accept a friend request
    private fun acceptRequest(request: FriendRequest) {
        firestore.collection("friend_requests").document(request.id)
            .update("status", "accepted")
            .addOnSuccessListener {
                val friendshipData = mapOf(
                    "user1Id" to request.senderId,
                    "user2Id" to request.receiverId,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("friendships").add(friendshipData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Friend request accepted", Toast.LENGTH_SHORT).show()
                        loadPendingRequests()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to add friendship", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to accept friend request", Toast.LENGTH_SHORT).show()
            }
    }

    // Decline a friend request
    private fun declineRequest(request: FriendRequest) {
        firestore.collection("friend_requests").document(request.id)
            .update("status", "declined")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Friend request declined", Toast.LENGTH_SHORT).show()
                loadPendingRequests()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to decline friend request", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
