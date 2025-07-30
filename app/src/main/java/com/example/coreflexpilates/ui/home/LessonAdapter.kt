package com.example.coreflexpilates.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coreflexpilates.R
import com.example.coreflexpilates.model.Lesson

class LessonAdapter(
    private val isAdmin: Boolean = false,
    private val trainerNameMap: Map<String, String> = emptyMap(),
    private val onEditClick: ((Lesson) -> Unit)? = null,
    private val onDeleteClick: ((Lesson) -> Unit)? = null,
    private val onInviteClick: ((Lesson) -> Unit)? = null  // הוספתי פרמטר חדש לכפתור הזמנה
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    private val lessons = mutableListOf<Lesson>()

    inner class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.lessonTitle)
        val trainer: TextView = view.findViewById(R.id.lessonTrainer)
        val description: TextView = view.findViewById(R.id.lessonDescription)
        val buttonEdit: ImageButton? = view.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton? = view.findViewById(R.id.buttonDelete)
        val buttonInvite: ImageButton? = view.findViewById(R.id.buttonInviteFriends)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        holder.title.text = lesson.title
        holder.trainer.text = trainerNameMap[lesson.trainerId] ?: "Unknown Trainer"
        holder.description.text =
            "${lesson.schedule.date} · ${lesson.schedule.time} · ${lesson.bookedCount}/${lesson.capacity}"

        if (isAdmin) {
            holder.buttonEdit?.visibility = View.VISIBLE
            holder.buttonDelete?.visibility = View.VISIBLE
            holder.buttonInvite?.visibility = View.GONE

            holder.buttonEdit?.setOnClickListener {
                onEditClick?.invoke(lesson)
            }

            holder.buttonDelete?.setOnClickListener {
                onDeleteClick?.invoke(lesson)
            }

        } else {
            holder.buttonEdit?.visibility = View.GONE
            holder.buttonDelete?.visibility = View.GONE
            holder.buttonInvite?.visibility = View.VISIBLE

            holder.itemView.setOnClickListener {
                // ניווט לדיטיילס - אפשר גם להעביר דרך onEditClick אם רוצים
            }

            holder.buttonInvite?.setOnClickListener {
                onInviteClick?.invoke(lesson)
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
