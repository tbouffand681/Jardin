package com.jardin.semis.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jardin.semis.SemisApplication
import com.jardin.semis.SemisViewModel
import com.jardin.semis.databinding.BottomSheetPlantDetailBinding
import com.jardin.semis.ui.calendar.AddSowingBottomSheet
import kotlinx.coroutines.launch

class PlantDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlantDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private var plantId: Long = -1L

    companion object {
        fun newInstance(plantId: Long) = PlantDetailBottomSheet().apply {
            this.plantId = plantId
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPlantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val p = (requireActivity().application as SemisApplication)
                .repository.getPlantById(plantId) ?: return@launch

            with(binding) {
                tvEmoji.text = p.emoji
                tvName.text = p.name
                tvLatinName.text = p.latinName
                tvCategory.text = p.category
                tvOccupationDays.text = "${p.occupationDays} jours d'occupation du sol"
                tvGermination.text = "Germination : ${p.germinationDays} jours (${p.germinationTempMin}–${p.germinationTempMax}°C)"
                tvSpacing.text = "Espacement : ${p.spacingCm} cm"
                tvSunExposure.text = "Exposition : ${p.sunExposure}"
                tvWaterNeeds.text = "Arrosage : ${p.waterNeeds}"

                val months = p.sowingMonths.split(",").mapNotNull { it.trim().toIntOrNull() }
                val monthNames = months.joinToString(" · ") { monthToName(it) }
                tvSowingMonths.text = if (monthNames.isNotEmpty()) "Semis : $monthNames" else "Semis : non défini"

                tvNotes.text = p.notes.ifEmpty { "Aucune note." }
                tvNotes.visibility = View.VISIBLE

                btnSowNow.setOnClickListener {
                    dismiss()
                    AddSowingBottomSheet().show(parentFragmentManager, "AddSowing")
                }
            }
        }
    }

    private fun monthToName(m: Int) = when (m) {
        1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"; 5 -> "Mai"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aoû"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Déc"
        else -> ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
