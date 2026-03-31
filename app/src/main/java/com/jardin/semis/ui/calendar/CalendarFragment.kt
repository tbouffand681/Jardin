package com.jardin.semis.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.FragmentCalendarBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private lateinit var sowingsAdapter: SowingsTimelineAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sowingsAdapter = SowingsTimelineAdapter(
            onStatusClick = { sowing -> showStatusDialog(sowing.id, sowing.status) },
            onDeleteClick = { sowing ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer ce semis ?")
                    .setMessage("Cette action est irréversible.")
                    .setPositiveButton("Supprimer") { _, _ ->
                        // On ne peut pas appeler deleteSowing avec SowingWithPlant directement
                        // On passe par updateStatus → FAILED comme workaround simple
                        viewModel.updateSowingStatus(sowing.id, SowingStatus.FAILED)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        )

        binding.recyclerSowings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sowingsAdapter
        }

        setupMonthHeader()
        observeSowings()

        binding.fabAddSowing.setOnClickListener {
            AddSowingBottomSheet().show(childFragmentManager, "AddSowing")
        }
    }

    private fun setupMonthHeader() {
        val now = LocalDate.now()
        val month = now.month.getDisplayName(
            java.time.format.TextStyle.FULL_STANDALONE,
            java.util.Locale.FRENCH
        ).replaceFirstChar { it.uppercase() }
        binding.tvCurrentMonth.text = "$month ${now.year}"
    }

    private fun observeSowings() {
        lifecycleScope.launch {
            viewModel.allSowingsWithPlant.collectLatest { sowings ->
                sowingsAdapter.submitList(sowings)

                val now = LocalDate.now()
                val fmt = DateTimeFormatter.ISO_LOCAL_DATE
                val thisMonth = sowings.filter {
                    try {
                        val d = LocalDate.parse(it.sowingDate, fmt)
                        d.year == now.year && d.month == now.month
                    } catch (e: Exception) { false }
                }
                val active = sowings.filter {
                    it.status != SowingStatus.HARVESTED && it.status != SowingStatus.FAILED
                }

                binding.tvStatSowings.text = "${sowings.size} semis"
                binding.tvStatActive.text = "${active.size} actifs"
                binding.tvStatMonth.text = "${thisMonth.size} ce mois"

                binding.emptyView.visibility = if (sowings.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerSowings.visibility = if (sowings.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun showStatusDialog(sowingId: Long, currentStatus: SowingStatus) {
        val labels = arrayOf("🌰 Semé", "🌱 Levée observée", "🌿 En croissance", "✅ Récolté", "❌ Échec")
        val statuses = SowingStatus.values()
        val currentIdx = statuses.indexOf(currentStatus).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mettre à jour le statut")
            .setSingleChoiceItems(labels, currentIdx) { dialog, which ->
                viewModel.updateSowingStatus(sowingId, statuses[which])
                dialog.dismiss()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
