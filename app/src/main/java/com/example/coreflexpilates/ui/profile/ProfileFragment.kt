package com.example.coreflexpilates.ui.profile

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var editName: EditText
    private lateinit var spinnerLevel: Spinner
    private lateinit var editGoal: EditText
    private lateinit var buttonSave: Button
    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var noLessonsText: TextView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val bookedLessons = mutableListOf<Lesson>()
    private lateinit var adapter: BookedLessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        editName = view.findViewById(R.id.editName)
        spinnerLevel = view.findViewById(R.id.spinnerLevel)
        editGoal = view.findViewById(R.id.editGoal)
        buttonSave = view.findViewById(R.id.buttonSave)
        bookingsRecyclerView = view.findViewById(R.id.recyclerViewBookings)
        noLessonsText = view.findViewById(R.id.textNoLessons)

        setupLevelSpinner()
        loadUserProfile()
        setupRecyclerView()
        loadBookedLessons()

        buttonSave.setOnClickListener {
            saveUserProfile()
        }

        return view
    }

    private fun setupLevelSpinner() {
        val levels = arrayOf("Beginner", "Intermediate", "Advanced")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    editName.setText(doc.getString("name") ?: "")
                    editGoal.setText(doc.getString("goal") ?: "")

                    val level = doc.getString("level") ?: "Beginner"
                    val position = (spinnerLevel.adapter as ArrayAdapter<String>).getPosition(level)
                    spinnerLevel.setSelection(position)
                }
            }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        val data = mapOf(
            "name" to editName.text.toString(),
            "goal" to editGoal.text.toString(),
            "level" to spinnerLevel.selectedItem.toString()
        )

        firestore.collection("users").document(userId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        adapter = BookedLessonAdapter(bookedLessons)
        bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        bookingsRecyclerView.adapter = adapter
    }

    private fun loadBookedLessons() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { bookingDocs ->
                val lessonIds = bookingDocs.mapNotNull { it.getString("lessonId") }

                if (lessonIds.isEmpty()) {
                    noLessonsText.visibility = View.VISIBLE
                    bookingsRecyclerView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                firestore.collection("classes")
                    .whereIn("classId", lessonIds.take(10)) // Firestore מגביל ל-10 תנאים ב־whereIn
                    .get()
                    .addOnSuccessListener { classDocs ->
                        bookedLessons.clear()
                        for (doc in classDocs) {
                            val lesson = doc.toObject(Lesson::class.java)
                            bookedLessons.add(lesson)
                        }
                        adapter.notifyDataSetChanged()

                        noLessonsText.visibility = if (bookedLessons.isEmpty()) View.VISIBLE else View.GONE
                        bookingsRecyclerView.visibility = if (bookedLessons.isEmpty()) View.GONE else View.VISIBLE
                    }
            }
    }
}
