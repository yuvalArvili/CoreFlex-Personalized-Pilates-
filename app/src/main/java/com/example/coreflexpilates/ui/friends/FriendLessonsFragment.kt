package com.example.coreflexpilates.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentFriendLessonsBinding
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.ui.home.LessonAdapter
import com.google.firebase.firestore.FirebaseFirestore

class FriendLessonsFragment : Fragment() {

    private var _binding: FragmentFriendLessonsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val lessons = mutableListOf<Lesson>()
    private lateinit var adapter: LessonAdapter

    private lateinit var friendId: String
    private lateinit var friendName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendId = arguments?.getString("friendId") ?: ""
        friendName = arguments?.getString("friendName") ?: "Friend"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendLessonsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set title showing friend's name
        binding.friendLessonsTitle.text = "$friendName's Lessons"

        // Setup toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }


        adapter = LessonAdapter(
            isAdmin = false,
            onEditClick = {},
            onDeleteClick = {},
            onInviteClick = { lesson ->
                // Navigate to invite friends screen for the selected lesson
                val action = FriendLessonsFragmentDirections.actionFriendLessonsFragmentToInviteFriendsFragment(lesson.classId)
                findNavController().navigate(action)
            }
        )
        binding.recyclerViewFriendLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriendLessons.adapter = adapter

        // Load the friend's booked lessons from Firestore
        loadLessons()
    }

    private fun loadLessons() {
        // Query bookings where userId matches friendId
        db.collection("bookings")
            .whereEqualTo("userId", friendId)
            .get()
            .addOnSuccessListener { result ->
                // Extract lessons from bookings
                val lessonIds = result.mapNotNull { it.getString("lessonId") }

                if (lessonIds.isEmpty()) return@addOnSuccessListener

                db.collection("lessons")
                    .whereIn("classId", lessonIds)
                    .get()
                    .addOnSuccessListener { lessonDocs ->
                        lessons.clear()
                        for (doc in lessonDocs) {
                            doc.toObject(Lesson::class.java)?.let { lessons.add(it) }
                        }
                        adapter.updateData(lessons.toList()) // Update adapter with fetched lessons
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
