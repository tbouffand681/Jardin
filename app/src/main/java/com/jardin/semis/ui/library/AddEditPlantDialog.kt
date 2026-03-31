package com.jardin.semis.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jardin.semis.SemisApplication
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.DialogAddEditPlantBinding

class AddEditPlantDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditPlantBinding? = null
    private val binding get() = _binding!!

    // ViewModel partagé avec l'activité — factory explicite pour Android 14
    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    // Plante à modifier (null = création)
    private var existingPlant: Plant? = null

    companion object {
        fun newInstance(plant: Plant) = AddEditPlantDialog().apply {
            existingPlant = plant
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingPlant?.let { plant ->
            binding.tvDialogTitle.text = "Modifier ${plant.name}"
            binding.etName.setText(plant.name)
            binding.etLatinName.setText(plant.latinName)
            binding.etEmoji.setText(plant.emoji)
            binding.etCategory.setText(plant.category)
            binding.etSowingMonths.setText(plant.sowingMonths)
            binding.etOccupationDays.setText(plant.occupationDays.toString())
            binding.etSpacingCm.setText(plant.spacingCm.toString())
            binding.etGerminationDays.setText(plant.germinationDays.toString())
            binding.etNotes.setText(plant.notes)
        } ?: run {
            binding.tvDialogTitle.text = "🌱 Nouvelle plante"
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                binding.tilName.error = "Le nom est requis"
                return@setOnClickListener
            }
            binding.tilName.error = null

            val basePlant = existingPlant ?: Plant(name = name)
            val plant = basePlant.copy(
                name = name,
                latinName = binding.etLatinName.text?.toString()?.trim() ?: "",
                emoji = binding.etEmoji.text?.toString()?.trim()?.ifEmpty { "🌱" } ?: "🌱",
                category = binding.etCategory.text?.toString()?.trim()?.ifEmpty { "Légume" } ?: "Légume",
                sowingMonths = binding.etSowingMonths.text?.toString()?.trim() ?: "",
                occupationDays = binding.etOccupationDays.text?.toString()?.toIntOrNull() ?: 90,
                spacingCm = binding.etSpacingCm.text?.toString()?.toIntOrNull() ?: 30,
                germinationDays = binding.etGerminationDays.text?.toString()?.toIntOrNull() ?: 10,
                notes = binding.etNotes.text?.toString()?.trim() ?: "",
                isDefault = existingPlant?.isDefault ?: false
            )

            try {
                if (existingPlant != null) {
                    viewModel.updatePlant(plant)
                } else {
                    viewModel.addPlant(plant)
                }
                if (isAdded && !isStateSaved) dismissAllowingStateLoss()
            } catch (e: Exception) {
                // Afficher l'erreur sans crasher
                binding.tilName.error = "Erreur : ${e.message}"
            }
        }

        binding.btnCancel.setOnClickListener { if (isAdded && !isStateSaved) dismissAllowingStateLoss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
