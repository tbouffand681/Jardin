package com.jardin.semis.ui.journal

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.NaturalEvent
import com.jardin.semis.databinding.FragmentJournalBinding
import com.jardin.semis.databinding.BottomSheetAddEventBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private lateinit var adapter: NaturalEventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NaturalEventAdapter { event ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer cette observation ?")
                .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteNaturalEvent(event) }
                .setNegativeButton("Annuler", null)
                .show()
        }

        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEvents.adapter = adapter

        lifecycleScope.launch {
            viewModel.allNaturalEvents.collectLatest { events ->
                adapter.submitList(events)
                binding.emptyView.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerEvents.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
                binding.tvEventCount.text = "${events.size} observation${if (events.size > 1) "s" else ""}"
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        binding.fabAddEvent.setOnClickListener {
            AddEventBottomSheet().show(childFragmentManager, "AddEvent")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── BottomSheet ajout événement ──────────────────────────────────────────────

class AddEventBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEventBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SemisViewModel by activityViewModels()
    private var selectedDate: LocalDate = LocalDate.now()
    private val displayFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    private val categories = listOf("Flore 🌸", "Faune 🦜", "Météo 🌦️", "Insecte 🦋", "Champignon 🍄", "Autre 📝")
    private val categoryEmojis = mapOf(
        "Flore 🌸" to "🌸", "Faune 🦜" to "🦜", "Météo 🌦️" to "🌦️",
        "Insecte 🦋" to "🦋", "Champignon 🍄" to "🍄", "Autre 📝" to "📝"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSelectedDate.text = selectedDate.format(displayFmt)

        val catAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(catAdapter)
        binding.spinnerCategory.setText(categories[1], false) // Faune par défaut

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                binding.tvSelectedDate.text = selectedDate.format(displayFmt)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnConfirm.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) { binding.tilTitle.error = "Titre requis"; return@setOnClickListener }

            val catLabel = binding.spinnerCategory.text.toString()
            val emoji = categoryEmojis[catLabel] ?: "🌿"
            val category = catLabel.substringBefore(" ")

            val event = NaturalEvent(
                eventDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                title = title,
                category = category,
                emoji = emoji,
                description = binding.etDescription.text.toString().trim(),
                location = binding.etLocation.text.toString().trim()
            )
            viewModel.addNaturalEvent(event)
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
