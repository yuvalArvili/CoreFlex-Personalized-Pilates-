package com.example.coreflexpilates.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.DayItem
import java.time.LocalDate

class DayAdapter(
    private val days: List<DayItem>,
    private var selectedIndex: Int = 0,
    private val onDaySelected: (DayItem) -> Unit
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
                notifyItemChanged(selectedIndex)
                onDaySelected(dayItem)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayName: TextView = itemView.findViewById(R.id.textDayName)
        private val dayNumber: TextView = itemView.findViewById(R.id.textDayNumber)

        fun bind(dayItem: DayItem, isSelected: Boolean) {
            dayName.text = dayItem.name
            dayNumber.text = dayItem.number.toString()

            val context = itemView.context
            itemView.background = if (isSelected) {
                ContextCompat.getDrawable(context, R.drawable.bg_day_selected)
            } else {
                null
            }
        }
    }

    fun updateSelectedDateByValue(date: LocalDate) {
        val newIndex = days.indexOfFirst { it.fullDate == date }
        if (newIndex != -1 && newIndex != selectedIndex) {
            val previous = selectedIndex
            selectedIndex = newIndex
            notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
        }
    }

}
