package com.example.coreflexpilates.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.R
import com.example.coreflexpilates.databinding.FragmentHomeBinding
import com.example.coreflexpilates.model.Lesson
import com.example.coreflexpilates.model.DayItem
import com.google.firebase.firestore.FirebaseFirestore
import java.time.*
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.time.temporal.ChronoUnit


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val allLessons = mutableListOf<Lesson>()
    private lateinit var lessonAdapter: LessonAdapter

    private var selectedDate: LocalDate = LocalDate.now()
    private var currentWeekOffset = 0
    private lateinit var dayAdapter: DayAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup lessons
        lessonAdapter = LessonAdapter()
        binding.recyclerViewLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLessons.adapter = lessonAdapter

        // Fetch data
        fetchLessons()

        // Setup days
        setupRecyclerViewDays(currentWeekOffset)

        // Filter menu
        binding.buttonFilter.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.filter_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.filter_all -> {
                        lessonAdapter.updateData(allLessons)
                        true
                    }
                    R.id.filter_beginners -> {
                        filterLessons("beginners")
                        true
                    }
                    R.id.filter_intermediate -> {
                        filterLessons("intermediate")
                        true
                    }
                    R.id.filter_advanced -> {
                        filterLessons("advanced")
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        // Calendar picker
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
                today.year,
                today.monthValue - 1,
                today.dayOfMonth
            )
            datePicker.show()
        }

        return root
    }

    private fun fetchLessons() {
        firestore.collection("classes")
            .get()
            .addOnSuccessListener { result ->
                allLessons.clear()
                for (doc in result) {
                    val lesson = doc.toObject(Lesson::class.java)
                    allLessons.add(lesson)
                }
                filterLessonsByDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching lessons", e)
            }
    }

    private fun filterLessons(level: String) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
