package com.jardin.semis.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.ItemPlantCardBinding

class PlantAdapter(
    private val onPlantClick: (Plant) -> Unit,
    private val onEditClick: (Plant) -> Unit
) : ListAdapter<Plant, PlantAdapter.PlantViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlantViewHolder(private val binding: ItemPlantCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plant: Plant) {
            with(binding) {
                tvEmoji.text = plant.emoji
                tvPlantName.text = plant.name
                tvLatinName.text = plant.latinName
                tvCategory.text = plant.category
                tvOccupation.text = "⏱ ${plant.occupationDays}j"
                tvSunExposure.text = when (plant.sunExposure) {
                    "Plein soleil" -> "☀️ Plein soleil"
                    "Mi-ombre" -> "⛅ Mi-ombre"
                    else -> "🌑 Ombre"
                }
                tvWaterNeeds.text = when (plant.waterNeeds) {
                    "Faible" -> "💧 Faible"
                    "Élevé" -> "💧💧💧 Élevé"
                    else -> "💧💧 Moyen"
                }

                // Mois de semis
                val months = plant.sowingMonths.split(",").mapNotNull { it.trim().toIntOrNull() }
                val monthNames = months.map { monthNumberToShort(it) }.joinToString(" ")
                tvSowingMonths.text = if (monthNames.isNotEmpty()) "📅 $monthNames" else ""

                root.setOnClickListener { onPlantClick(plant) }
                btnEdit.setOnClickListener { onEditClick(plant) }
            }
        }

        private fun monthNumberToShort(month: Int) = when (month) {
            1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"; 5 -> "Mai"
            6 -> "Jun"; 7 -> "Jul"; 8 -> "Aoû"; 9 -> "Sep"; 10 -> "Oct"
            11 -> "Nov"; 12 -> "Déc"; else -> ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Plant>() {
        override fun areItemsTheSame(oldItem: Plant, newItem: Plant) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Plant, newItem: Plant) = oldItem == newItem
    }
}
