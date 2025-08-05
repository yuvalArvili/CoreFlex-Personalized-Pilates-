package com.example.coreflexpilates.ui.lesson

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Booking
import com.example.coreflexpilates.model.Lesson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LessonsFragment : Fragment() {

    private lateinit var recyclerViewBookings: RecyclerView
    private lateinit var textNoLessons: TextView
    private lateinit var buttonUpcoming: Button
    private lateinit var buttonPast: Button

    private val lessons = mutableListOf<Lesson>() // Lessons to display
    private lateinit var adapter: BookedLessonAdapter

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Flag to control upcoming or past lessons
    private var isShowingUpcoming = true

    // Parse date and time strings into timestamps
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_lessons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewBookings = view.findViewById(R.id.recyclerViewBookings)
        textNoLessons = view.findViewById(R.id.textNoLessons)
        buttonUpcoming = view.findViewById(R.id.buttonUpcoming)
        buttonPast = view.findViewById(R.id.buttonPast)


        adapter = BookedLessonAdapter(lessons) {
            loadLessons()
        }
        recyclerViewBookings.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewBookings.adapter = adapter

        // Toggle buttons to switch between upcoming and past lessons
        buttonUpcoming.setOnClickListener {
            isShowingUpcoming = true
            updateButtons()
            loadLessons()
        }

        buttonPast.setOnClickListener {
            isShowingUpcoming = false
            updateButtons()
            loadLessons()
        }

        updateButtons()
        loadLessons()
    }

    // Update button UI on current filter
    private fun updateButtons() {
        buttonUpcoming.isSelected = isShowingUpcoming
        buttonPast.isSelected = !isShowingUpcoming
    }

    // Load lessons from Firestore filtered by user's bookings and upcoming/past state
    private fun loadLessons() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { bookingDocs ->
                val bookings = bookingDocs.mapNotNull { it.toObject(Booking::class.java) }
                val lessonIds = bookings.mapNotNull { it.lessonId }

                if (lessonIds.isEmpty()) {// No bookings found, show empty state
                    lessons.clear()
                    adapter.notifyDataSetChanged()
                    textNoLessons.visibility = View.VISIBLE
                    recyclerViewBookings.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Firestore limits "whereIn" queries to 10 items, so take up to 10 lesson IDs
                firestore.collection("lessons")
                    .whereIn("classId", lessonIds.take(10))
                    .get()
                    .addOnSuccessListener { classDocs ->
                        lessons.clear()
                        val now = System.currentTimeMillis()

                        for (doc in classDocs) {
                            val lesson = doc.toObject(Lesson::class.java)
                            val lessonDateTime = "${lesson.schedule.date} ${lesson.schedule.time}"
                            val lessonTime = try {// Parse datetime string into timestamp
                                formatter.parse(lessonDateTime)?.time
                            } catch (e: Exception) {
                                null
                            }

                            Log.d("LessonsFragment", "Lesson: ${lesson.title}, dateTime: $lessonDateTime, timeMillis: $lessonTime")

                            // Determine if lesson time is in the future
                            val isFuture = (lessonTime ?: 0) > now

                            Log.d("LessonsFragment", "isFuture: $isFuture, isShowingUpcoming: $isShowingUpcoming")

                            // Add lesson to list only if it matches current filter (upcoming/past)
                            if ((isShowingUpcoming && isFuture) || (!isShowingUpcoming && !isFuture)) {
                                lessons.add(lesson)
                            }
                        }

                        // Sort lessons by date and time (lex order works since yyyy-MM-dd format)
                        lessons.sortBy { it.schedule.date + it.schedule.time }

                        // Update adapter and toggle empty state visibility
                        adapter.notifyDataSetChanged()
                        textNoLessons.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE
                        recyclerViewBookings.visibility = if (lessons.isEmpty()) View.GONE else View.VISIBLE
                    }
            }
    }
}
