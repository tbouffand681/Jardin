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
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private var existingPlant: Plant? = null

    companion object {
        val CATEGORIES = listOf(
            "Légume", "Légume fruit", "Légume racine", "Légume feuille",
            "Légumineuse", "Aromate", "Bulbe", "Tubercule", "Fleur", "Autre"
        )
        val SUN_OPTIONS = listOf("☀️ Plein soleil", "⛅ Mi-ombre", "🌑 Ombre")
        val WATER_OPTIONS = listOf("💧 Faible", "💧💧 Moyen", "💧💧💧 Élevé")

        fun newInstance(plant: Plant) = AddEditPlantDialog().apply { existingPlant = plant }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Remplir les dropdowns
        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, CATEGORIES)
        binding.spinnerCategory.setAdapter(catAdapter)

        val sunAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, SUN_OPTIONS)
        binding.spinnerSun.setAdapter(sunAdapter)

        val waterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, WATER_OPTIONS)
        binding.spinnerWater.setAdapter(waterAdapter)

        // Remplir le formulaire si modification
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
            // Matcher l'exposition et l'eau avec les options du dropdown
            val sunMatch = SUN_OPTIONS.firstOrNull { it.contains(p.sunExposure, ignoreCase = true) } ?: SUN_OPTIONS[0]
            binding.spinnerSun.setText(sunMatch, false)
            val waterMatch = WATER_OPTIONS.firstOrNull { it.contains(p.waterNeeds, ignoreCase = true) } ?: WATER_OPTIONS[1]
            binding.spinnerWater.setText(waterMatch, false)
            binding.etNotes.setText(p.notes)
            // Bouton supprimer visible seulement en mode édition
            binding.btnDeletePlant.visibility = View.VISIBLE
        } ?: run {
            binding.tvDialogTitle.text = "🌱 Nouvelle plante"
            // Valeurs par défaut
            binding.spinnerCategory.setText(CATEGORIES[0], false)
            binding.spinnerSun.setText(SUN_OPTIONS[0], false)
            binding.spinnerWater.setText(WATER_OPTIONS[1], false)
        }

        binding.btnDeletePlant.setOnClickListener {
            val plant = existingPlant ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer ${plant.name} ?")
                .setMessage("Cette plante sera définitivement retirée de la bibliothèque.")
                .setPositiveButton("Supprimer") { _, _ ->
                    viewModel.deletePlant(plant)
                    if (isAdded && !isStateSaved) dismissAllowingStateLoss()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) { binding.tilName.error = "Le nom est requis"; return@setOnClickListener }
            binding.tilName.error = null

            // Extraire l'exposition et l'eau depuis les dropdowns (retirer l'emoji)
            val sunRaw = binding.spinnerSun.text?.toString() ?: SUN_OPTIONS[0]
            val sunExposure = when {
                sunRaw.contains("Plein soleil") -> "Plein soleil"
                sunRaw.contains("Mi-ombre") -> "Mi-ombre"
                else -> "Ombre"
            }
            val waterRaw = binding.spinnerWater.text?.toString() ?: WATER_OPTIONS[1]
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

            if (existingPlant != null) viewModel.updatePlant(plant) else viewModel.addPlant(plant)
            if (isAdded && !isStateSaved) dismissAllowingStateLoss()
        }

        binding.btnCancel.setOnClickListener { if (isAdded && !isStateSaved) dismissAllowingStateLoss() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
