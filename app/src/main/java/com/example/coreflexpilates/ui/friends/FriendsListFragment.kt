package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.R
import com.example.coreflexpilates.databinding.FragmentFriendsListBinding
import com.example.coreflexpilates.model.Friendship
import com.example.coreflexpilates.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsListFragment : Fragment() {

    private var _binding: FragmentFriendsListBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val friends = mutableListOf<User>()
    private lateinit var adapter: FriendsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = FriendsListAdapter(friends) { selectedFriend ->
            val bundle = Bundle().apply {
                putString("friendId", selectedFriend.uid)
                putString("friendName", selectedFriend.name)
            }

            val fragment = FriendLessonsFragment()
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriends.adapter = adapter

        loadFriends()
    }

    private fun loadFriends() {
        val currentUserId = auth.currentUser?.uid ?: return

        val friendIds = mutableSetOf<String>()

        firestore.collection("friendships")
            .whereEqualTo("user1Id", currentUserId)
            .get()
            .addOnSuccessListener { result1 ->
                result1.forEach { doc -> friendIds.add(doc.getString("user2Id") ?: "") }

                firestore.collection("friendships")
                    .whereEqualTo("user2Id", currentUserId)
                    .get()
                    .addOnSuccessListener { result2 ->
                        result2.forEach { doc -> friendIds.add(doc.getString("user1Id") ?: "") }

                        if (friendIds.isEmpty()) return@addOnSuccessListener

                        firestore.collection("users")
                            .whereIn("uid", friendIds.toList())
                            .get()
                            .addOnSuccessListener { usersSnapshot ->
                                friends.clear()
                                for (doc in usersSnapshot.documents) {
                                    doc.toObject(User::class.java)?.let { friends.add(it) }
                                }
                                adapter.notifyDataSetChanged()
                            }
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
