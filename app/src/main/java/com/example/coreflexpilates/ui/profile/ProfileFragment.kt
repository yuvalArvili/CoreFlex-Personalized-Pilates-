    package com.example.coreflexpilates.ui.profile

    import android.app.AlertDialog
    import android.content.Intent
    import android.os.Bundle
    import android.view.*
    import android.widget.*
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.coreflexpilates.AuthActivity
    import com.example.coreflexpilates.R
    import com.example.coreflexpilates.model.Lesson
    import com.example.coreflexpilates.model.Booking
    import com.google.android.material.button.MaterialButton
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore

    class ProfileFragment : Fragment() {

        private lateinit var editName: EditText
        private lateinit var spinnerLevel: Spinner
        private lateinit var editGoal: EditText
        private lateinit var buttonSave: Button
        private lateinit var bookingsRecyclerView: RecyclerView
        private lateinit var noLessonsText: TextView
        private lateinit var buttonUpcoming: MaterialButton
        private lateinit var buttonPast: MaterialButton

        private val auth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()

        private val bookedLessons = mutableListOf<Lesson>()
        private lateinit var adapter: BookedLessonAdapter

        private var isShowingUpcoming = true

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
            buttonUpcoming = view.findViewById(R.id.buttonUpcoming)
            buttonPast = view.findViewById(R.id.buttonPast)

            setupLevelSpinner()
            loadUserProfile()
            setupRecyclerView()

            // Set default state
            buttonUpcoming.isSelected = true
            buttonPast.isSelected = false
            isShowingUpcoming = true

            loadBookedLessons()

            buttonSave.setOnClickListener {
                saveUserProfile()
            }

            buttonUpcoming.setOnClickListener {
                isShowingUpcoming = true
                buttonUpcoming.isSelected = true
                buttonPast.isSelected = false
                loadBookedLessons()
            }

            buttonPast.setOnClickListener {
                isShowingUpcoming = false
                buttonUpcoming.isSelected = false
                buttonPast.isSelected = true
                loadBookedLessons()
            }

            val logoutButton = view.findViewById<Button>(R.id.buttonLogout)
            logoutButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes") { _, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(requireContext(), AuthActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
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
            val now = System.currentTimeMillis()

            firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { bookingDocs ->
                    val filteredBookings = bookingDocs.mapNotNull { it.toObject(Booking::class.java) }
                        .filter { booking ->
                            val isFuture = booking.timestamp > now
                            (isShowingUpcoming && isFuture) || (!isShowingUpcoming && !isFuture)
                        }

                    val lessonIds = filteredBookings.mapNotNull { it.lessonId }

                    if (lessonIds.isEmpty()) {
                        bookedLessons.clear()
                        adapter.notifyDataSetChanged()
                        noLessonsText.visibility = View.VISIBLE
                        bookingsRecyclerView.visibility = View.GONE
                        return@addOnSuccessListener
                    }

                    firestore.collection("classes")
                        .whereIn("classId", lessonIds.take(10))
                        .get()
                        .addOnSuccessListener { classDocs ->
                            bookedLessons.clear()
                            for (doc in classDocs) {
                                val lesson = doc.toObject(Lesson::class.java)
                                bookedLessons.add(lesson)
                            }

                            // Sort by schedule date + time for better UX
                            bookedLessons.sortBy { it.schedule.date + it.schedule.time }

                            adapter.notifyDataSetChanged()
                            noLessonsText.visibility = if (bookedLessons.isEmpty()) View.VISIBLE else View.GONE
                            bookingsRecyclerView.visibility = if (bookedLessons.isEmpty()) View.GONE else View.VISIBLE
                        }
                }
        }
    }
