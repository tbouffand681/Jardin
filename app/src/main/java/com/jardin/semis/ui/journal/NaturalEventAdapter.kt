package com.jardin.semis.ui.journal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jardin.semis.data.model.NaturalEvent
import com.jardin.semis.databinding.ItemNaturalEventBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class NaturalEventAdapter(
    private val onDeleteClick: (NaturalEvent) -> Unit
) : ListAdapter<NaturalEvent, NaturalEventAdapter.ViewHolder>(DiffCallback()) {

    private val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemNaturalEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemNaturalEventBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(event: NaturalEvent) {
            b.tvEmoji.text = event.emoji
            b.tvTitle.text = event.title
            b.tvCategory.text = event.category
            b.tvDate.text = try {
                LocalDate.parse(event.eventDate, DateTimeFormatter.ISO_LOCAL_DATE).format(displayFmt)
            } catch (e: Exception) { event.eventDate }
            b.tvDescription.text = event.description.ifEmpty { "" }
            b.tvDescription.visibility = if (event.description.isEmpty()) ViewGroup.GONE else ViewGroup.VISIBLE
            b.tvLocation.text = if (event.location.isNotEmpty()) "📍 ${event.location}" else ""
            b.tvLocation.visibility = if (event.location.isEmpty()) ViewGroup.GONE else ViewGroup.VISIBLE
            b.btnDelete.setOnClickListener { onDeleteClick(event) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NaturalEvent>() {
        override fun areItemsTheSame(a: NaturalEvent, b: NaturalEvent) = a.id == b.id
        override fun areContentsTheSame(a: NaturalEvent, b: NaturalEvent) = a == b
    }
}
