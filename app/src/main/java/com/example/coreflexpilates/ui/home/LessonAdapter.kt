package com.example.coreflexpilates.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson

class LessonAdapter :
    RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    private val lessons = mutableListOf<Lesson>()

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.lessonTitle)
        val description: TextView = itemView.findViewById(R.id.lessonDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        holder.title.text = lesson.title
        holder.description.text =
            "Date: ${lesson.schedule.date}, Time: ${lesson.schedule.time}, Location: ${lesson.location}"

        // navigate to lessonDetails
        holder.itemView.setOnClickListener {
            val action = HomeFragmentDirections
                .actionHomeFragmentToLessonDetailsFragment(lesson.classId)

            it.findNavController().navigate(action)
        }
    }

    override fun getItemCount() = lessons.size

    fun updateData(newLessons: List<Lesson>) {
        lessons.clear()
        lessons.addAll(newLessons)
        notifyDataSetChanged()
    }
}
