package com.example.coreflexpilates.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentAdminUsersBinding
import com.example.coreflexpilates.model.User
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private val userList = mutableListOf<User>()
    private val filteredUserList = mutableListOf<User>()
    private val userImagesMap = mutableMapOf<String, String?>()

    private lateinit var adapter: AdminUsersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AdminUsersAdapter(filteredUserList, userImagesMap) { user ->
            // Navigate to user subscription management on user click
            val action = AdminUsersFragmentDirections
                .actionAdminUsersFragmentToUserSubscriptionFragment(user.uid)
            findNavController().navigate(action)
        }

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.adapter = adapter

        // Setup search view to filter user list
        setupSearch()
        loadUsers()
    }

    private fun setupSearch() {
        binding.searchViewUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // No special action on submit
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter user list as user types
                filterUsers(newText ?: "")
                return true
            }
        })
    }

    private fun loadUsers() {
        // Fetch all user documents from Firestore users collection
        db.collection("users").get()
            .addOnSuccessListener { result ->
                // Clear current lists/maps
                userList.clear()
                userImagesMap.clear()

                for (doc in result) {
                    val user = doc.toObject(User::class.java).apply { uid = doc.id }
                    // Exclude admin users from this list
                    if (user.role != "admin") {
                        userList.add(user)
                        // Extract and store profile image URL if available
                        val imageUrl = doc.getString("profileImageUrl")
                        userImagesMap[user.uid] = imageUrl
                    }
                }
                // Initialize filtered list to show all users initially
                filteredUserList.clear()
                filteredUserList.addAll(userList)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterUsers(query: String) {
        // Convert query to lowercase for case-insensitive search
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        filteredUserList.clear()
        if (lowerCaseQuery.isEmpty()) {
            // If search box is empty, show all users
            filteredUserList.addAll(userList)
        } else {
            // Filter users whose names contain the query substring
            filteredUserList.addAll(userList.filter {
                it.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            })
        }
        // Notify adapter to update the displayed list accordingly
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
