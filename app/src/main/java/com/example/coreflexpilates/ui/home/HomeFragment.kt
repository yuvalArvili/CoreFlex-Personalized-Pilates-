package com.example.coreflexpilates.ui.home

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentHomeBinding
import com.example.coreflexpilates.model.DayItem
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.model.Trainer
import com.example.coreflexpilates.ui.admin.EditLessonActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.time.*
import com.google.firebase.functions.FirebaseFunctions
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val allLessons = mutableListOf<Lesson>()
    private lateinit var lessonAdapter: LessonAdapter
    private val trainerNameMap = mutableMapOf<String, String>()

    private var selectedDate: LocalDate = LocalDate.now()
    private var currentWeekOffset = 0
    private lateinit var dayAdapter: DayAdapter

    private var isAdmin: Boolean = false

    companion object {
        private val LEVELS = mapOf(
            "All" to "",
            "PILATES | beginners" to "Beginners",
            "PILATES +| intermediate" to "Intermediate",
            "PILATES ++| advanced" to "Advanced"
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAdmin = arguments?.getBoolean("isAdmin", false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // Initialize lesson adapter with callbacks for admin/user actions
        lessonAdapter = LessonAdapter(
            isAdmin = isAdmin,
            trainerNameMap = trainerNameMap,
            onEditClick = { lesson ->
                val intent = Intent(requireContext(), EditLessonActivity::class.java)
                intent.putExtra("lessonId", lesson.classId)
                startActivity(intent)
            },
            onDeleteClick = { lesson -> confirmDelete(lesson) },
            onInviteClick = { lesson ->
                val action = HomeFragmentDirections.actionHomeFragmentToInviteFriendsFragment(lesson.classId)
                findNavController().navigate(action)
            },
            onLessonClick = { lesson ->
                val action = HomeFragmentDirections.actionHomeFragmentToLessonDetailsFragment(lesson.classId)
                findNavController().navigate(action)
            }
        )

        // Setup recycler view for lessons
        binding.recyclerViewLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLessons.adapter = lessonAdapter

        fetchLessons()
        fetchTrainerNames()

        setupRecyclerViewDays(currentWeekOffset)

        binding.buttonFilter.setOnClickListener { view ->
            showMainFilterMenu(view)
        }


        binding.buttonOpenCalendar.setOnClickListener {
            val today = LocalDate.now()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    filterLessonsByDate(selectedDate)

                    // Calculate week offset for day selector
                    val baseSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    val selectedSunday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    currentWeekOffset = ChronoUnit.WEEKS.between(baseSunday, selectedSunday).toInt()
                    setupRecyclerViewDays(currentWeekOffset)
                },
                today.year, today.monthValue - 1, today.dayOfMonth
            )
            datePicker.show()
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        // Refresh lessons when fragment resumes
        fetchLessons()
    }

    // Load all lessons from Firestore, sort, and filter by selected date
    private fun fetchLessons() {
        firestore.collection("lessons")
            .get()
            .addOnSuccessListener { result ->
                allLessons.clear()
                for (doc in result) {
                    val lesson = doc.toObject(Lesson::class.java)?.copy(classId = doc.id)
                    if (lesson != null) {
                        allLessons.add(lesson)
                    }
                }
                allLessons.sortWith(compareBy({ it.schedule.date }, { it.schedule.time }))
                filterLessonsByDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching lessons", e)
            }
    }

    // Load all trainer names into map for display
    private fun fetchTrainerNames() {
        firestore.collection("trainers").get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val trainer = doc.toObject(Trainer::class.java)
                    trainerNameMap[doc.id] = trainer.name
                }
            }
    }

    private fun showMainFilterMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Filter by Level")
        popup.menu.add("Filter by Trainer")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Filter by Level" -> showLevelFilterMenu(anchor)
                "Filter by Trainer" -> showTrainerFilterMenu(anchor)
            }
            true
        }

        popup.show()
    }

    private fun showLevelFilterMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        LEVELS.forEach { (key, _) ->
            popup.menu.add(key)
        }

        popup.setOnMenuItemClickListener { item ->
            val level = item.title.toString()
            if (level == "All") {
                lessonAdapter.updateData(allLessons)
            } else {
                filterLessonsByLevel(level)
            }
            true
        }

        popup.show()
    }

    private fun showTrainerFilterMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("All")
        trainerNameMap.values.sorted().forEach { name ->
            popup.menu.add(name)
        }

        val trainerNameToId = trainerNameMap.entries.associate { it.value to it.key }

        popup.setOnMenuItemClickListener { item ->
            val selectedName = item.title.toString()
            if (selectedName == "All") {
                lessonAdapter.updateData(allLessons)
            } else {
                val trainerId = trainerNameToId[selectedName]
                val filtered = allLessons.filter { it.trainerId == trainerId }
                lessonAdapter.updateData(filtered)
            }
            true
        }

        popup.show()
    }

    // Filter lessons by level
    private fun filterLessonsByLevel(level: String) {
        val filtered = allLessons.filter { it.title == level }
            .sortedWith(compareBy({ it.schedule.date }, { it.schedule.time }))
        lessonAdapter.updateData(filtered)
    }

    // Filter lessons by date
    private fun filterLessonsByDate(date: LocalDate) {
        val isoDate = date.toString()
        val filtered = allLessons.filter { it.schedule.date == isoDate }
            .sortedWith(compareBy({ it.schedule.date }, { it.schedule.time }))
        lessonAdapter.updateData(filtered)
    }


    private fun setupRecyclerViewDays(offset: Int = 0) {
        val today = LocalDate.now().plusWeeks(offset.toLong())
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

        val days = (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            DayItem(
                name = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("he")),
                number = date.dayOfMonth,
                fullDate = date
            )
        }

        val initialSelectedIndex = days.indexOfFirst { it.fullDate == selectedDate }

        dayAdapter = DayAdapter(days, initialSelectedIndex) { selectedDay ->
            selectedDate = selectedDay.fullDate
            filterLessonsByDate(selectedDate)
        }

        binding.recyclerViewDays.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewDays.adapter = dayAdapter
    }

    // Show confirmation dialog before deleting a lesson (admin only)
    private fun confirmDelete(lesson: Lesson) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Lesson")
            .setMessage("Are you sure you want to delete this lesson?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLesson(lesson)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Delete lesson and update related bookings and subscriptions
    private fun deleteLesson(lesson: Lesson) {
        firestore.collection("lessons")
            .document(lesson.classId)
            .delete()
            .addOnSuccessListener {
                // Fetch all bookings for this lesson to update user quotas and send notifications
                firestore.collection("bookings")
                    .whereEqualTo("lessonId", lesson.classId)
                    .get()
                    .addOnSuccessListener { bookingsSnapshot ->
                        for (bookingDoc in bookingsSnapshot.documents) {
                            val userId = bookingDoc.getString("userId") ?: continue
                            Log.d("HomeFragment", "Sending cancel notification to userId: $userId")

                            val userRef = firestore.collection("users").document(userId)
                            // Increment subscription quota for affected users
                            firestore.runTransaction { transaction ->
                                val userSnapshot = transaction.get(userRef)
                                val currentQuota = userSnapshot.getLong("subscriptionQuota") ?: 0
                                transaction.update(userRef, "subscriptionQuota", currentQuota + 1)
                            }.addOnSuccessListener {
                                Log.d("HomeFragment", "Subscription quota incremented for userId: $userId")
                            }.addOnFailureListener { e ->
                                Log.e("HomeFragment", "Failed to increment subscription quota for userId: $userId", e)
                            }

                            // Prepare data for cancel notification
                            val data = HashMap<String, Any>()
                            data["userId"] = userId
                            data["lessonTitle"] = lesson.title
                            data["lessonDate"] = lesson.schedule.date
                            data["lessonTime"] = lesson.schedule.time

                            Log.d("HomeFragment", "Sending cancel notification data: $data")

                            // Call Firebase Cloud Function to send notification
                            FirebaseFunctions.getInstance()
                                .getHttpsCallable("sendBookingCancelledNotification")
                                .call(data)
                                .addOnSuccessListener {
                                    Log.d("HomeFragment", "Cancel notification sent successfully to $userId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HomeFragment", "Failed to send cancel notification", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Failed to get bookings for cancellation", e)
                    }

                // Refresh lessons list
                fetchLessons()
            }
            .addOnFailureListener {
                Log.e("HomeFragment", "Failed to delete lesson", it)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
