package com.example.coreflexpilates.ui.lesson

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LessonDetailsFragment : Fragment(R.layout.fragment_lesson_details) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentLesson: Lesson

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lessonId = arguments?.getString("lessonId") ?: return

        val titleText: TextView = view.findViewById(R.id.lessonTitleText)
        val detailsText: TextView = view.findViewById(R.id.lessonDetailsText)
        val bookButton: Button = view.findViewById(R.id.bookButton)

        firestore.collection("classes").document(lessonId)
            .get()
            .addOnSuccessListener { doc ->
                val lesson = doc.toObject(Lesson::class.java)
                if (lesson != null) {
                    currentLesson = lesson
                    titleText.text = lesson.title
                    detailsText.text =
                        "Date: ${lesson.schedule.date}\nTime: ${lesson.schedule.time}\nLocation: ${lesson.location}"

                    bookButton.setOnClickListener {
                        bookLesson(lesson)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load lesson", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bookLesson(lesson: Lesson) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingData = mapOf(
            "userId" to userId,
            "lessonId" to lesson.classId,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Lesson booked!", Toast.LENGTH_SHORT).show()
                checkPermissionAndAddEventToCalendar(lesson)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Booking failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPermissionAndAddEventToCalendar(lesson: Lesson) {
        val permission = android.Manifest.permission.WRITE_CALENDAR
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 1001)
        } else {
            addEventToCalendar(lesson)
        }
    }

    private fun addEventToCalendar(lesson: Lesson) {
        try {
            val dateParts = lesson.schedule.date.split("-")
            val timeParts = lesson.schedule.time.split(":")

            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
            }

            val startMillis = calendar.timeInMillis
            val endMillis = startMillis + 60 * 60 * 1000

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, lesson.title)
                put(CalendarContract.Events.DESCRIPTION, "Pilates class")
                put(CalendarContract.Events.EVENT_LOCATION, lesson.location)
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = requireContext().contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                Toast.makeText(requireContext(), "Lesson added to calendar!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to insert calendar event", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error adding to calendar", Toast.LENGTH_SHORT).show()
        }
    }
}
