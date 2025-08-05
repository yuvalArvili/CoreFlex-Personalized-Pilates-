package com.example.coreflexpilates.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.coreflexpilates.databinding.FragmentUserSubscriptionBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class UserSubscriptionFragment : Fragment() {

    private var _binding: FragmentUserSubscriptionBinding? = null
    private val binding get() = _binding!!

    private val args: UserSubscriptionFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()

    private var currentSubscription: String? = null
    private var userId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUserSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userId = args.userId

        loadUserData()

        // Save button update subscription info
        binding.buttonSaveSubscription.setOnClickListener {
            saveSubscription()
        }
    }

    // Fetch user name and subscription from Firestore
    private fun loadUserData() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Unknown User"
                    binding.textUserName.text = name

                    // Load current subscription and update radio buttons accordingly
                    currentSubscription = doc.getString("subscriptionFrequency") ?: "NONE"
                    updateRadioButtons(currentSubscription!!)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    // Updates the radio buttons UI to reflect the current subscription selection
    private fun updateRadioButtons(subscription: String) {
        when (subscription) {
            "ONCE_A_WEEK" -> {
                binding.radioOnceAWeek.isChecked = true
                binding.radioTwiceAWeek.isChecked = false
                binding.radioThreeTimesAWeek.isChecked = false
            }
            "TWICE_A_WEEK" -> {
                binding.radioOnceAWeek.isChecked = false
                binding.radioTwiceAWeek.isChecked = true
                binding.radioThreeTimesAWeek.isChecked = false
            }
            "THREE_TIMES_A_WEEK" -> {
                binding.radioOnceAWeek.isChecked = false
                binding.radioTwiceAWeek.isChecked = false
                binding.radioThreeTimesAWeek.isChecked = true
            }
            "NONE" -> {
                // No subscription selected
                binding.radioOnceAWeek.isChecked = false
                binding.radioTwiceAWeek.isChecked = false
                binding.radioThreeTimesAWeek.isChecked = false
            }
            else -> {
                binding.radioOnceAWeek.isChecked = false
                binding.radioTwiceAWeek.isChecked = false
                binding.radioThreeTimesAWeek.isChecked = false
            }
        }
    }

    // Saves the selected subscription
    private fun saveSubscription() {
        val selectedRadioButtonId = binding.radioGroupSubscription.checkedRadioButtonId

        // Determine selected subscription value based on checked radio button
        val selectedValue = if (selectedRadioButtonId == -1) {
            "NONE"
        } else {
            val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioButtonId)
            when (selectedRadioButton) {
                binding.radioOnceAWeek -> "ONCE_A_WEEK"
                binding.radioTwiceAWeek -> "TWICE_A_WEEK"
                binding.radioThreeTimesAWeek -> "THREE_TIMES_A_WEEK"
                else -> "NONE"
            }
        }

        // If no change
        if (selectedValue == currentSubscription) {
            Toast.makeText(requireContext(), "No changes to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate subscription expiry date 1 year from today
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.YEAR, 1)
        val expiryDate = calendar.time

        val dataToUpdate = mapOf(
            "subscriptionFrequency" to selectedValue,
            "subscriptionExpiry" to expiryDate
        )

        db.collection("users").document(userId)
            .update(dataToUpdate)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Subscription updated", Toast.LENGTH_SHORT).show()
                currentSubscription = selectedValue
                updateRadioButtons(currentSubscription!!)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update subscription", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
