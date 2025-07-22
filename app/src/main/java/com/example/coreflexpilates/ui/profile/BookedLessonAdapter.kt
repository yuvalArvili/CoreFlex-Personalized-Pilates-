package com.example.coreflexpilates.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson

class BookedLessonAdapter(private val lessons: List<Lesson>) :
    RecyclerView.Adapter<BookedLessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.lessonTitle)
        val details: TextView = itemView.findViewById(R.id.lessonDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.title.text = lesson.title
        holder.details.text = "Date: ${lesson.schedule.date}   Time: ${lesson.schedule.time}   Location: ${lesson.location}"
    }

    override fun getItemCount(): Int = lessons.size
}
