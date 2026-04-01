package com.jardin.semis.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.DialogAddEditPlantBinding

class AddEditPlantDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditPlantBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private var existingPlant: Plant? = null

    companion object {
        val CATEGORIES = listOf(
            "Légume", "Légume fruit", "Légume racine", "Légume feuille",
            "Légumineuse", "Aromate", "Bulbe", "Tubercule", "Fleur", "Autre"
        )
        val SUN_OPTIONS = listOf("Plein soleil", "Mi-ombre", "Ombre")
        val WATER_OPTIONS = listOf("Faible", "Moyen", "Élevé")

        fun newInstance(plant: Plant) = AddEditPlantDialog().apply { existingPlant = plant }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, CATEGORIES)
        binding.spinnerCategory.setAdapter(catAdapter)

        val sunLabels = listOf("☀️ Plein soleil", "⛅ Mi-ombre", "🌑 Ombre")
        val sunAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sunLabels)
        binding.spinnerSun.setAdapter(sunAdapter)

        val waterLabels = listOf("💧 Faible", "💧💧 Moyen", "💧💧💧 Élevé")
        val waterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, waterLabels)
        binding.spinnerWater.setAdapter(waterAdapter)

        existingPlant?.let { p ->
            binding.tvDialogTitle.text = "✏️ Modifier ${p.name}"
            binding.etName.setText(p.name)
            binding.etLatinName.setText(p.latinName)
            binding.etEmoji.setText(p.emoji)
            binding.spinnerCategory.setText(p.category, false)
            binding.etSowingMonths.setText(p.sowingMonths)
            binding.etOccupationDays.setText(p.occupationDays.toString())
            binding.etSpacingCm.setText(p.spacingCm.toString())
            binding.etGerminationDays.setText(p.germinationDays.toString())
            // Trouver le label correspondant à l'exposition stockée
            val sunIdx = SUN_OPTIONS.indexOfFirst { p.sunExposure.contains(it, true) }.coerceAtLeast(0)
            binding.spinnerSun.setText(sunLabels[sunIdx], false)
            val waterIdx = WATER_OPTIONS.indexOfFirst { p.waterNeeds.contains(it, true) }.coerceAtLeast(1)
            binding.spinnerWater.setText(waterLabels[waterIdx], false)
            binding.etNotes.setText(p.notes)
            binding.btnDeletePlant.visibility = View.VISIBLE
        } ?: run {
            binding.tvDialogTitle.text = "🌱 Nouvelle plante"
            binding.spinnerCategory.setText(CATEGORIES[0], false)
            binding.spinnerSun.setText(sunLabels[0], false)
            binding.spinnerWater.setText(waterLabels[1], false)
        }

        binding.btnDeletePlant.setOnClickListener {
            val plant = existingPlant ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer ${plant.name} ?")
                .setMessage("Cette plante sera retirée de la bibliothèque.")
                .setPositiveButton("Supprimer") { _, _ ->
                    viewModel.deletePlant(plant) {
                        // dismiss dans le callback, sur le main thread
                        activity?.runOnUiThread {
                            if (isAdded && !isStateSaved) dismissAllowingStateLoss()
                        }
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) { binding.tilName.error = "Le nom est requis"; return@setOnClickListener }
            binding.tilName.error = null

            // Désactiver le bouton pendant l'opération
            binding.btnSave.isEnabled = false

            val sunRaw = binding.spinnerSun.text?.toString() ?: ""
            val sunExposure = when {
                sunRaw.contains("Plein soleil") -> "Plein soleil"
                sunRaw.contains("Mi-ombre") -> "Mi-ombre"
                else -> "Ombre"
            }
            val waterRaw = binding.spinnerWater.text?.toString() ?: ""
            val waterNeeds = when {
                waterRaw.contains("Élevé") -> "Élevé"
                waterRaw.contains("Faible") -> "Faible"
                else -> "Moyen"
            }

            val base = existingPlant ?: Plant(name = name)
            val plant = base.copy(
                name = name,
                latinName = binding.etLatinName.text?.toString()?.trim() ?: "",
                emoji = binding.etEmoji.text?.toString()?.trim()?.ifEmpty { "🌱" } ?: "🌱",
                category = binding.spinnerCategory.text?.toString()?.trim()?.ifEmpty { "Légume" } ?: "Légume",
                sowingMonths = binding.etSowingMonths.text?.toString()?.trim() ?: "",
                occupationDays = binding.etOccupationDays.text?.toString()?.toIntOrNull() ?: 90,
                spacingCm = binding.etSpacingCm.text?.toString()?.toIntOrNull() ?: 30,
                germinationDays = binding.etGerminationDays.text?.toString()?.toIntOrNull() ?: 10,
                sunExposure = sunExposure,
                waterNeeds = waterNeeds,
                notes = binding.etNotes.text?.toString()?.trim() ?: "",
                isDefault = existingPlant?.isDefault ?: false
            )

            val onDone = { success: Boolean ->
                activity?.runOnUiThread {
                    if (success) {
                        if (isAdded && !isStateSaved) dismissAllowingStateLoss()
                    } else {
                        _binding?.btnSave?.isEnabled = true
                    }
                }
            }

            if (existingPlant != null) viewModel.updatePlant(plant, onDone)
            else viewModel.addPlant(plant, onDone)
        }

        binding.btnCancel.setOnClickListener {
            if (isAdded && !isStateSaved) dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
