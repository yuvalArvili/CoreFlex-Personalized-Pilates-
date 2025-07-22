package com.example.coreflexpilates.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson

class LessonAdapter(
    private val isAdmin: Boolean = false,
    private val onEditClick: ((Lesson) -> Unit)? = null,
    private val onDeleteClick: ((Lesson) -> Unit)? = null
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    private val lessons = mutableListOf<Lesson>()

    inner class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.lessonTitle)
        val description: TextView = view.findViewById(R.id.lessonDescription)
        val buttonEdit: ImageButton? = view.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton? = view.findViewById(R.id.buttonDelete)
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
            "${lesson.schedule.date} · ${lesson.schedule.time} · ${lesson.location}"

        if (isAdmin) {
            holder.buttonEdit?.visibility = View.VISIBLE
            holder.buttonDelete?.visibility = View.VISIBLE

            holder.buttonEdit?.setOnClickListener {
                onEditClick?.invoke(lesson)
            }

            holder.buttonDelete?.setOnClickListener {
                onDeleteClick?.invoke(lesson)
            }
        } else {
            holder.buttonEdit?.visibility = View.GONE
            holder.buttonDelete?.visibility = View.GONE

            holder.itemView.setOnClickListener {
                val action = HomeFragmentDirections
                    .actionHomeFragmentToLessonDetailsFragment(lesson.classId)
                it.findNavController().navigate(action)
            }
        }
    }

    override fun getItemCount(): Int = lessons.size

    fun updateData(newLessons: List<Lesson>) {
        lessons.clear()
        lessons.addAll(newLessons)
        notifyDataSetChanged()
    }
}
