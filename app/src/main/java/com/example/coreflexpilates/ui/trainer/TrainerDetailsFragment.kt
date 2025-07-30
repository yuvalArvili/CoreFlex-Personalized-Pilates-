package com.example.coreflexpilates.ui.trainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.coreflexpilates.databinding.FragmentTrainerDetailsBinding
import com.example.coreflexpilates.model.Trainer
import com.google.firebase.firestore.FirebaseFirestore

class TrainerDetailsFragment : Fragment() {

    private var _binding: FragmentTrainerDetailsBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val trainerId = arguments?.getString("trainerId") ?: return
        loadTrainerDetails(trainerId)
    }

    private fun loadTrainerDetails(trainerId: String) {
        firestore.collection("trainers").document(trainerId)
            .get()
            .addOnSuccessListener { document ->
                val trainer = document.toObject(Trainer::class.java)
                if (trainer != null) {
                    binding.trainerNameText.text = trainer.name
                    binding.trainerSpecialtiesText.text = "Specialties: ${trainer.specialties.joinToString()}"

                    // Load image from imageUrl field (if exists)
                    trainer.imageUrl?.let { imageUrl ->
                        Glide.with(this)
                            .load(imageUrl)
                            .into(binding.imageTrainer)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load trainer", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
