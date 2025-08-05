package com.example.coreflexpilates.ui.lesson

import android.app.AlertDialog
import android.content.ContentUris
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookedLessonAdapter(
    private val lessons: List<Lesson>,
    private val onCancelSuccess: () -> Unit
) : RecyclerView.Adapter<BookedLessonAdapter.LessonViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.lessonTitle)
        val trainer: TextView = itemView.findViewById(R.id.lessonTrainer)
        val details: TextView = itemView.findViewById(R.id.lessonDescription)
        val cancelButton: ImageButton = itemView.findViewById(R.id.buttonCancelBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        Log.d("BookedLessonAdapter", "Binding lesson: ${lesson.title}")

        // Set lesson title and details
        holder.title.text = lesson.title
        holder.details.text =
            "Date: ${lesson.schedule.date}   Time: ${lesson.schedule.time}  ${lesson.bookedCount}/${lesson.capacity}"

        // Load trainer's name from Firestore
        loadTrainerName(lesson.trainerId) { name ->
            holder.trainer.text = "Trainer: $name"
        }

        // Show cancel booking button
        holder.cancelButton.visibility = View.VISIBLE
        holder.cancelButton.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel this lesson?")
                .setPositiveButton("Yes") { _, _ ->
                    cancelBooking(lesson, holder)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    // Load trainer name from Firestore by trainer ID
    private fun loadTrainerName(trainerId: String, callback: (String) -> Unit) {
        firestore.collection("trainers").document(trainerId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown"
                callback(name)
            }
            .addOnFailureListener {
                callback("Unknown")
            }
    }

    // Cancel booking in Firestore
    private fun cancelBooking(lesson: Lesson, holder: LessonViewHolder) {
        val userId = auth.currentUser?.uid ?: return

        // Find booking document for this user and lesson
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereEqualTo("lessonId", lesson.classId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val bookingDoc = doc.reference
                    val calendarEventId = doc.getLong("calendarEventId")

                    // Delete booking document
                    bookingDoc.delete()
                        .addOnSuccessListener {
                            val lessonRef = firestore.collection("lessons").document(lesson.classId)
                            val userRef = firestore.collection("users").document(userId)

                            // Calculate current week and year for weekly quota update
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                            val lessonDate = LocalDate.parse(lesson.schedule.date, formatter)
                            val weekOfYear = lessonDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                            val year = lessonDate.year
                            val quotaDocId = "$year-$weekOfYear"

                            // Decrement booked count in lesson document
                            lessonRef.get()
                                .addOnSuccessListener { lessonSnap ->
                                    val currentBooked = lessonSnap.getLong("bookedCount") ?: 0
                                    lessonRef.update("bookedCount", (currentBooked - 1).coerceAtLeast(0))
                                }

                            // Decrement user's weekly quota count transactionally
                            val quotaRef = firestore.collection("users")
                                .document(userId)
                                .collection("weeklyQuotas")
                                .document(quotaDocId)

                            firestore.runTransaction { transaction ->
                                val snap = transaction.get(quotaRef)
                                val currentCount = snap.getLong("count") ?: 0
                                val newCount = (currentCount - 1).coerceAtLeast(0)
                                transaction.set(quotaRef, mapOf("count" to newCount))
                            }

                            // Increment user's subscription quota
                            userRef.get()
                                .addOnSuccessListener { userSnap ->
                                    val currentQuota = userSnap.getLong("subscriptionQuota") ?: 0
                                    userRef.update("subscriptionQuota", currentQuota + 1)
                                }

                            Toast.makeText(holder.itemView.context, "Booking cancelled and subscription updated", Toast.LENGTH_SHORT).show()

                            // refresh UI
                            onCancelSuccess()

                            // Remove calendar event if exists
                            if (calendarEventId != null) {
                                val uri = ContentUris.withAppendedId(
                                    CalendarContract.Events.CONTENT_URI,
                                    calendarEventId
                                )
                                try {
                                    holder.itemView.context.contentResolver.delete(uri, null, null)
                                    Toast.makeText(holder.itemView.context, "Removed from calendar", Toast.LENGTH_SHORT).show()
                                } catch (e: SecurityException) {
                                    Toast.makeText(holder.itemView.context, "No permission to remove calendar event", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(holder.itemView.context, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    override fun getItemCount(): Int = lessons.size
}

