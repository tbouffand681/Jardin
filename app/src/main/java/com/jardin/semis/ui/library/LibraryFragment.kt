package com.jardin.semis.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.jardin.semis.SemisViewModel
import com.jardin.semis.databinding.FragmentLibraryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private lateinit var plantAdapter: PlantAdapter
    private var selectedCategory: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        plantAdapter = PlantAdapter(
            onPlantClick = { plant ->
                PlantDetailBottomSheet.newInstance(plant.id)
                    .show(childFragmentManager, "PlantDetail")
            },
            onEditClick = { plant ->
                AddEditPlantDialog.newInstance(plant)
                    .show(childFragmentManager, "EditPlant")
            }
        )

        binding.recyclerPlants.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = plantAdapter
        }

        setupSearch()
        observeCategories()
        observePlants()

        binding.fabAddPlant.setOnClickListener {
            AddEditPlantDialog().show(childFragmentManager, "AddPlant")
        }
    }

    private fun setupSearch() {
        binding.searchBar.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString() ?: ""
            lifecycleScope.launch {
                if (query.isEmpty()) {
                    observePlants()
                } else {
                    viewModel.searchPlants(query).collectLatest {
                        plantAdapter.submitList(it)
                        binding.tvPlantCount.text = "${it.size} plante${if (it.size > 1) "s" else ""}"
                    }
                }
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            viewModel.allCategories.collectLatest { categories ->
                binding.chipGroupCategories.removeAllViews()

                val allChip = Chip(requireContext()).apply {
                    text = "Tout"
                    isCheckable = true
                    isChecked = selectedCategory == null
                    setOnClickListener {
                        selectedCategory = null
                        observePlants()
                    }
                }
                binding.chipGroupCategories.addView(allChip)

                categories.forEach { category ->
                    val chip = Chip(requireContext()).apply {
                        text = category
                        isCheckable = true
                        isChecked = selectedCategory == category
                        setOnClickListener {
                            selectedCategory = category
                            lifecycleScope.launch {
                                viewModel.getPlantsByCategory(category).collectLatest {
                                    plantAdapter.submitList(it)
                                    binding.tvPlantCount.text = "${it.size} plante${if (it.size > 1) "s" else ""}"
                                }
                            }
                        }
                    }
                    binding.chipGroupCategories.addView(chip)
                }
            }
        }
    }

    private fun observePlants() {
        lifecycleScope.launch {
            viewModel.allPlants.collectLatest { plants ->
                plantAdapter.submitList(plants)
                binding.tvPlantCount.text = "${plants.size} plante${if (plants.size > 1) "s" else ""}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
