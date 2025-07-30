package com.example.coreflexpilates.ui.admin

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Trainer

class AdminTrainerAdapter(
    private val trainers: List<Trainer>,
    private val onEditClick: (Trainer) -> Unit,
    private val onDeleteClick: (Trainer) -> Unit
) : RecyclerView.Adapter<AdminTrainerAdapter.TrainerViewHolder>() {

    inner class TrainerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val trainerImage: ImageView = view.findViewById(R.id.trainerImage)
        val trainerName: TextView = view.findViewById(R.id.trainerName)
        val trainerSpecialty: TextView = view.findViewById(R.id.trainerSpecialty)
        val editButton: ImageButton = view.findViewById(R.id.editTrainerButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteTrainerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_trainer, parent, false)
        return TrainerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        val trainer = trainers[position]

        holder.trainerName.text = trainer.name
        holder.trainerSpecialty.text = trainer.specialties.joinToString(", ")

        Glide.with(holder.itemView.context)
            .load(trainer.imageUrl)
            .placeholder(R.drawable.baseline_perm_identity_24)
            .into(holder.trainerImage)

        holder.editButton.setOnClickListener {
            onEditClick(trainer)
        }

        holder.deleteButton.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Trainer")
                .setMessage("Are you sure you want to delete ${trainer.name}?")
                .setPositiveButton("Yes") { _, _ ->
                    onDeleteClick(trainer)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = trainers.size
}
