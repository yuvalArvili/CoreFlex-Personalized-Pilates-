package com.example.coreflexpilates.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.DayItem

class DayAdapter(
    private val days: List<DayItem>,
    private var selectedIndex: Int = 0,
    private val onDaySelected: (DayItem) -> Unit       // Callback when a day is selected
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayItem = days[position]

        holder.bind(dayItem, position == selectedIndex)

        holder.itemView.setOnClickListener {
            if (position != selectedIndex) {
                val previous = selectedIndex
                selectedIndex = position
                notifyItemChanged(previous)
                notifyItemChanged(selectedIndex) // Refresh new selected day UI
                onDaySelected(dayItem)
            }
        }
    }

    override fun getItemCount(): Int = days.size


    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayName: TextView = itemView.findViewById(R.id.textDayName)
        private val dayNumber: TextView = itemView.findViewById(R.id.textDayNumber)

        // Bind the day name and number, and set background if selected
        fun bind(dayItem: DayItem, isSelected: Boolean) {
            dayName.text = dayItem.name
            dayNumber.text = dayItem.number.toString()

            val context = itemView.context
            itemView.background = if (isSelected) {
                ContextCompat.getDrawable(context, R.drawable.bg_day_selected) // Highlight background for selected day
            } else {
                null // No special background for unselected days
            }
        }
    }

}

