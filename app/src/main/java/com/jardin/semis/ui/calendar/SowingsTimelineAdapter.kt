package com.jardin.semis.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jardin.semis.R
import com.jardin.semis.data.db.SowingWithPlant
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.ItemSowingBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class SowingsTimelineAdapter(
    private val onStatusClick: (SowingWithPlant) -> Unit,
    private val onDeleteClick: (SowingWithPlant) -> Unit
) : ListAdapter<SowingWithPlant, SowingsTimelineAdapter.SowingViewHolder>(DiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SowingViewHolder {
        val binding = ItemSowingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SowingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SowingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SowingViewHolder(private val binding: ItemSowingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sowing: SowingWithPlant) {
            with(binding) {
                tvEmoji.text = sowing.plantEmoji
                tvPlantName.text = sowing.plantName
                tvLocation.text = if (sowing.location.isNotEmpty()) "📍 ${sowing.location}" else "📍 Non spécifié"

                val sowDate = LocalDate.parse(sowing.sowingDate, dateFormatter)
                val harvestDate = LocalDate.parse(sowing.expectedHarvestDate, dateFormatter)
                val today = LocalDate.now()

                tvSowingDate.text = "Semé le ${sowDate.format(displayFormatter)}"
                tvHarvestDate.text = "Récolte prévue : ${harvestDate.format(displayFormatter)}"

                // Calcul du progrès
                val totalDays = ChronoUnit.DAYS.between(sowDate, harvestDate).toFloat()
                val elapsedDays = ChronoUnit.DAYS.between(sowDate, today).coerceIn(0, totalDays.toLong()).toFloat()
                val progress = if (totalDays > 0) (elapsedDays / totalDays * 100).toInt() else 0
                progressBar.progress = progress.coerceIn(0, 100)

                val daysLeft = ChronoUnit.DAYS.between(today, harvestDate)
                tvDaysLeft.text = when {
                    daysLeft < 0 -> "Prêt à récolter ! 🎉"
                    daysLeft == 0L -> "Récolte aujourd'hui !"
                    daysLeft <= 7 -> "Dans $daysLeft jours"
                    else -> "Dans $daysLeft jours"
                }

                // Statut
                tvStatus.text = when (sowing.status) {
                    SowingStatus.SOWED -> "🌰 Semé"
                    SowingStatus.GERMINATED -> "🌱 Levée"
                    SowingStatus.GROWING -> "🌿 Croissance"
                    SowingStatus.HARVESTED -> "✅ Récolté"
                    SowingStatus.FAILED -> "❌ Échec"
                }

                // Couleur selon statut
                val statusColor = when (sowing.status) {
                    SowingStatus.SOWED -> R.color.status_sowed
                    SowingStatus.GERMINATED -> R.color.status_germinated
                    SowingStatus.GROWING -> R.color.status_growing
                    SowingStatus.HARVESTED -> R.color.status_harvested
                    SowingStatus.FAILED -> R.color.status_failed
                }
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))

                btnUpdateStatus.setOnClickListener { onStatusClick(sowing) }
                btnDelete.setOnClickListener { onDeleteClick(sowing) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SowingWithPlant>() {
        override fun areItemsTheSame(oldItem: SowingWithPlant, newItem: SowingWithPlant) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SowingWithPlant, newItem: SowingWithPlant) =
            oldItem == newItem
    }
}
