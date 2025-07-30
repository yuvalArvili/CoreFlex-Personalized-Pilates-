package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentFriendRequestsBinding
import com.example.coreflexpilates.model.FriendRequest
import com.example.coreflexpilates.model.Friendship
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requests = mutableListOf<FriendRequest>()
    private lateinit var adapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = FriendRequestAdapter(requests,
            onAcceptClick = { request -> acceptRequest(request) },
            onDeclineClick = { request -> declineRequest(request) }
        )
        binding.recyclerViewFriendRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriendRequests.adapter = adapter

        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("friend_requests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                requests.clear()
                for (doc in result) {
                    requests.add(doc.toObject(FriendRequest::class.java))
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun acceptRequest(request: FriendRequest) {
        val docRef = db.collection("friend_requests")
            .whereEqualTo("senderId", request.senderId)
            .whereEqualTo("receiverId", request.receiverId)
            .limit(1)

        docRef.get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val doc = result.documents[0].reference
                doc.update("status", "accepted")

                // Create friendship
                val friendship = Friendship(
                    user1Id = request.senderId,
                    user2Id = request.receiverId
                )

                db.collection("friendships").add(friendship)
            }
        }

        loadFriendRequests()
    }

    private fun declineRequest(request: FriendRequest) {
        val docRef = db.collection("friend_requests")
            .whereEqualTo("senderId", request.senderId)
            .whereEqualTo("receiverId", request.receiverId)
            .limit(1)

        docRef.get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val doc = result.documents[0].reference
                doc.update("status", "declined")
            }
        }

        loadFriendRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
