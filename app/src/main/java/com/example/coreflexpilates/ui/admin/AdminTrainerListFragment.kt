package com.example.coreflexpilates.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coreflexpilates.databinding.FragmentAdminTrainerListBinding
import com.example.coreflexpilates.model.Trainer
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.SearchView

class AdminTrainerListFragment : Fragment() {

    private var _binding: FragmentAdminTrainerListBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val trainerList = mutableListOf<Trainer>()
    private lateinit var allTrainers: List<Trainer>
    private lateinit var adapter: AdminTrainerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminTrainerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminTrainerAdapter(
            trainerList,
            onEditClick = { trainer ->
                val intent = Intent(requireContext(), EditTrainerActivity::class.java)
                intent.putExtra("trainerId", trainer.id)
                startActivity(intent)
            },
            onDeleteClick = { trainer -> deleteTrainer(trainer.id) }
        )

        binding.recyclerViewTrainers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTrainers.adapter = adapter

        setupSearch()
        loadTrainers()
    }

    private fun setupSearch() {
        binding.searchViewTrainer.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTrainers(newText ?: "")
                return true
            }
        })
    }

    private fun loadTrainers() {
        db.collection("trainers").get()
            .addOnSuccessListener { result ->
                allTrainers = result.mapNotNull { it.toObject(Trainer::class.java).apply { id = it.id } }
                trainerList.clear()
                trainerList.addAll(allTrainers)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load trainers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterTrainers(query: String) {
        val filtered = allTrainers.filter {
            it.name.contains(query, ignoreCase = true)
        }

        trainerList.clear()
        trainerList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun deleteTrainer(id: String) {
        db.collection("trainers").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Trainer deleted", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
