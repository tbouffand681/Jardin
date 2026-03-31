package com.jardin.semis.ui.calendar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.BottomSheetAddSowingBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddSowingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddSowingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private var selectedDate: LocalDate = LocalDate.now()
    private var plantList: List<Plant> = emptyList()
    private val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddSowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDate.text = selectedDate.format(displayFormatter)

        // Charger les plantes une seule fois
        lifecycleScope.launch {
            try {
                val plants = viewModel.allPlants.first()
                plantList = plants
                val names = plants.map { "${it.emoji} ${it.name}" }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                binding.spinnerPlant.setAdapter(adapter)
                binding.spinnerPlant.setOnItemClickListener { _, _, position, _ ->
                    if (position < plants.size) updateHarvestPreview(plants[position])
                }
            } catch (e: Exception) {
                // liste vide en cas d'erreur
            }
        }

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                binding.tvSelectedDate.text = selectedDate.format(displayFormatter)
                updateHarvestPreview()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnConfirm.setOnClickListener {
            val plantName = binding.spinnerPlant.text?.toString() ?: ""
            val plant = plantList.firstOrNull { "${it.emoji} ${it.name}" == plantName }

            if (plant == null) {
                binding.tilPlant.error = "Choisissez une plante"
                return@setOnClickListener
            }
            binding.tilPlant.error = null

            val location = binding.etLocation.text?.toString()?.trim() ?: ""
            val quantity = binding.etQuantity.text?.toString()?.toIntOrNull() ?: 1
            val notes = binding.etNotes.text?.toString()?.trim() ?: ""

            viewModel.addSowing(plant.id, selectedDate, location, quantity, notes)
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun updateHarvestPreview(plant: Plant? = null) {
        val p = plant ?: plantList.firstOrNull {
            "${it.emoji} ${it.name}" == binding.spinnerPlant.text?.toString()
        }
        p?.let {
            val harvest = selectedDate.plusDays(it.occupationDays.toLong())
            binding.tvHarvestPreview.text = "📅 Récolte estimée : ${harvest.format(displayFormatter)}"
            binding.tvHarvestPreview.visibility = View.VISIBLE
            binding.tvOccupationDays.text = "⏱ Occupation sol : ${it.occupationDays} jours"
            binding.tvOccupationDays.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
