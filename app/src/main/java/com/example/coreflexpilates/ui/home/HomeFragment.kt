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
import com.example.coreflexpilates.R
import com.example.coreflexpilates.databinding.FragmentHomeBinding
import com.example.coreflexpilates.model.DayItem
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.model.Trainer
import com.example.coreflexpilates.ui.admin.EditLessonActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.time.*
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
    private val trainerNameMap = mutableMapOf<String, String>() // trainerId -> name

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

        fun newInstance(isAdmin: Boolean): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putBoolean("isAdmin", isAdmin)
            fragment.arguments = args
            return fragment
        }
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
            }
        )


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
        fetchLessons()
    }

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
                filterLessonsByDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching lessons", e)
            }
    }

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

    private fun filterLessonsByLevel(level: String) {
        val filtered = allLessons.filter { it.title == level }
        lessonAdapter.updateData(filtered)
    }

    private fun filterLessonsByDate(date: LocalDate) {
        val isoDate = date.toString()
        val filtered = allLessons.filter { it.schedule.date == isoDate }
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

    private fun deleteLesson(lesson: Lesson) {
        firestore.collection("lessons")
            .document(lesson.classId)
            .delete()
            .addOnSuccessListener {
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
