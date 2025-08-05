package com.example.coreflexpilates.ui.lesson

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.*

class LessonDetailsFragment : Fragment(R.layout.fragment_lesson_details) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentLesson: Lesson
    private var pendingLessonForCalendar: Lesson? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val lessonId = arguments?.getString("lessonId") ?: return

        val titleText: TextView = view.findViewById(R.id.lessonTitleText)
        val detailsText: TextView = view.findViewById(R.id.lessonDetailsText)
        val bookButton: Button = view.findViewById(R.id.bookButton)
        val trainerText: TextView = view.findViewById(R.id.trainerNameText)

        // Load lesson details from Firestore by lessonId
        firestore.collection("lessons").document(lessonId)
            .get()
            .addOnSuccessListener { doc ->
                val lesson = doc.toObject(Lesson::class.java)
                if (lesson != null) {
                    currentLesson = lesson
                    titleText.text = lesson.title
                    detailsText.text =
                        " ${lesson.schedule.date} • ${lesson.schedule.time} • ${lesson.bookedCount}/${lesson.capacity}"

                    // Navigate to trainer details
                    trainerText.text = "View Trainer Details"
                    trainerText.setOnClickListener {
                        val bundle = Bundle().apply {
                            putString("trainerId", lesson.trainerId)
                        }
                        findNavController().navigate(
                            R.id.action_lessonDetailsFragment_to_trainerDetailsFragment,
                            bundle
                        )
                    }

                    // Handle booking button click (security)
                    bookButton.setOnClickListener {
                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Check if subscription valid
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val subscriptionFrequency = userDoc.getString("subscriptionFrequency") ?: "ONCE_A_WEEK"
                                val subscriptionExpiryTimestamp = userDoc.getTimestamp("subscriptionExpiry")
                                val subscriptionExpiry = subscriptionExpiryTimestamp?.toDate()?.time ?: 0L
                                val now = System.currentTimeMillis()

                                if (subscriptionExpiry < now) {
                                    Toast.makeText(requireContext(), "Subscription expired. Please renew.", Toast.LENGTH_LONG).show()
                                    return@addOnSuccessListener
                                }

                                // Check booking limit for subscription and week of lesson
                                canBookLesson(userId, subscriptionFrequency, currentLesson.schedule.date) { canBook ->
                                    if (canBook) {
                                        bookLesson(currentLesson)
                                    } else {
                                        Toast.makeText(requireContext(), "You have reached the booking limit for your subscription this week.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to check subscription", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load lesson", Toast.LENGTH_SHORT).show()
            }
    }

    private fun canBookLesson(userId: String, subscriptionFrequency: String, lessonDateStr: String, callback: (Boolean) -> Unit) {
        val maxLessons = getMaxLessonsForSubscription(subscriptionFrequency)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val lessonDate = LocalDate.parse(lessonDateStr, formatter)

        // Calculate start and end of week (Sunday to Saturday) for the lesson date
        val startOfWeek = lessonDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val endOfWeek = lessonDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { bookingsSnapshot ->
                val bookingsInWeek = bookingsSnapshot.documents.count { doc ->
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    // Convert timestamp to LocalDate
                    val bookingDate = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000))

                    // Check if bookingDate falls within the lesson's week
                    !bookingDate.isBefore(startOfWeek) && !bookingDate.isAfter(endOfWeek)
                }
                callback(bookingsInWeek < maxLessons)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // Returns max allowed bookings per week for subscription type
    private fun getMaxLessonsForSubscription(subscriptionFrequency: String): Int {
        return when (subscriptionFrequency) {
            "ONCE_A_WEEK" -> 1
            "TWICE_A_WEEK" -> 2
            "THREE_TIMES_A_WEEK" -> 3
            else -> 1
        }
    }

    private fun bookLesson(lesson: Lesson) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingsRef = firestore.collection("bookings")
        // Check if user already booked this lesson
        bookingsRef
            .whereEqualTo("lessonId", lesson.classId)
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    Toast.makeText(
                        requireContext(),
                        "You already booked this lesson",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->

                        val lessonRef = firestore.collection("lessons").document(lesson.classId)

                        // Safely update booked count and create booking document
                        firestore.runTransaction { transaction ->
                            val snapshot = transaction.get(lessonRef)
                            val currentBooked = snapshot.getLong("bookedCount") ?: 0
                            val capacity = snapshot.getLong("capacity") ?: 0

                            if (currentBooked >= capacity) {
                                throw Exception("Lesson is full")
                            }

                            // Increment booked count
                            transaction.update(lessonRef, "bookedCount", currentBooked + 1)

                            val calendarEventId = addEventToCalendarAndReturnId(lesson) // Add to calendar

                            // Create booking document with additional info
                            val bookingData = mapOf(
                                "userId" to userId,
                                "lessonId" to lesson.classId,
                                "timestamp" to System.currentTimeMillis(),
                                "fcmToken" to token,
                                "calendarEventId" to calendarEventId
                            )
                            bookingsRef.document().set(bookingData)

                        }.addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Lesson booked!",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }.addOnFailureListener { e ->
                            if (e.message?.contains("full") == true) {
                                Toast.makeText(
                                    requireContext(),
                                    "Lesson is already full",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Booking failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Failed to get FCM token",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check booking", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun addEventToCalendarAndReturnId(lesson: Lesson): Long? {
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
            val endMillis = startMillis + 60 * 60 * 1000 // 1-hour duration

            // Query for the primary calendar ID on device
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )
            val cursor = requireContext().contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val calendarId = cursor.getLong(0)
                cursor.close()

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, lesson.title)
                    put(CalendarContract.Events.DESCRIPTION, "Pilates class")
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

                // Insert event into calendar
                val uri = requireContext().contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    values
                )
                return uri?.lastPathSegment?.toLongOrNull()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            pendingLessonForCalendar?.let {
                addEventToCalendarAndReturnId(it)
            }
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
