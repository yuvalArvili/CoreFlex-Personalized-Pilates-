package com.example.coreflexpilates.ui.profile

import android.app.AlertDialog
import android.content.ContentUris
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson
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
        holder.title.text = lesson.title
        holder.details.text =
            "Date: ${lesson.schedule.date}   Time: ${lesson.schedule.time}  ${lesson.bookedCount}/${lesson.capacity}"

        loadTrainerName(lesson.trainerId) { name ->
            holder.trainer.text = "Trainer: $name"
        }

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

    private fun cancelBooking(lesson: Lesson, holder: LessonViewHolder) {
        val userId = auth.currentUser?.uid ?: return

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

                    bookingDoc.delete()

                    val lessonRef = firestore.collection("lessons").document(lesson.classId)
                    firestore.runTransaction { transaction ->
                        val snap = transaction.get(lessonRef)
                        val current = snap.getLong("bookedCount") ?: 0
                        transaction.update(lessonRef, "bookedCount", (current - 1).coerceAtLeast(0))
                    }.addOnSuccessListener {
                        Toast.makeText(holder.itemView.context, "Booking cancelled", Toast.LENGTH_SHORT).show()
                        onCancelSuccess()

                        // ✅ Remove from calendar if ID exists
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
                }
            }
    }

    override fun getItemCount(): Int = lessons.size
}
