package com.jardin.semis.ui.monthly

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.FragmentMonthlyBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MonthlyFragment : Fragment() {

    private var _binding: FragmentMonthlyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    private var currentMonth = LocalDate.now().monthValue

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateMonthDisplay()
        loadSowingsForMonth()
        binding.btnPrevMonth.setOnClickListener {
            currentMonth = if (currentMonth == 1) 12 else currentMonth - 1
            updateMonthDisplay(); loadSowingsForMonth()
        }
        binding.btnNextMonth.setOnClickListener {
            currentMonth = if (currentMonth == 12) 1 else currentMonth + 1
            updateMonthDisplay(); loadSowingsForMonth()
        }
        binding.btnShare.setOnClickListener { shareContent() }
        binding.btnPrint.setOnClickListener { printContent() }
    }

    private fun monthName() = LocalDate.of(2024, currentMonth, 1)
        .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH).replaceFirstChar { it.uppercase() }

    private fun updateMonthDisplay() {
        binding.tvMonthTitle.text = monthName()
        binding.tvMonthSubtitle.text = "Semis conseillés pour ${monthName()}"
    }

    private fun loadSowingsForMonth() {
        lifecycleScope.launch {
            val plants = viewModel.allPlants.first()
            val toSow = plants.filter { plant ->
                plant.sowingMonths.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(currentMonth)
            }.sortedBy { it.name }

            if (_binding == null) return@launch
            if (toSow.isEmpty()) {
                binding.emptyMonth.visibility = View.VISIBLE
                binding.contentMonth.visibility = View.GONE
            } else {
                binding.emptyMonth.visibility = View.GONE
                binding.contentMonth.visibility = View.VISIBLE
                binding.tvSowingList.text = buildSowingText(toSow)
                binding.tvSowingCount.text = "${toSow.size} plante${if (toSow.size > 1) "s" else ""} à semer"
            }
        }
    }

    private fun buildSowingText(plants: List<Plant>) = plants.joinToString("\n\n") { p ->
        buildString {
            append("${p.emoji} ${p.name}")
            if (p.latinName.isNotEmpty()) append(" (${p.latinName})")
            append("\n  ⏱ ${p.occupationDays} jours • ↔️ ${p.spacingCm} cm")
            append("\n  🌱 Germination : ${p.germinationDays} j à ${p.germinationTempMin}-${p.germinationTempMax}°C")
            append("\n  ☀️ ${p.sunExposure} • 💧 ${p.waterNeeds}")
            if (p.notes.isNotEmpty()) append("\n  📝 ${p.notes}")
        }
    }

    private fun shareContent() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Semis de ${monthName()}")
            putExtra(Intent.EXTRA_TEXT, "🌱 Almanach du jardin — Semis de ${monthName()}\n\n${binding.tvSowingList.text}")
        }
        startActivity(Intent.createChooser(intent, "Partager la liste de semis"))
    }

    private fun printContent() {
        // Charger les plantes PUIS construire le HTML (fix bug async)
        lifecycleScope.launch {
            val plants = viewModel.allPlants.first().filter { plant ->
                plant.sowingMonths.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(currentMonth)
            }.sortedBy { it.name }

            if (_binding == null) return@launch

            val html = buildString {
                append("<html><head><meta charset='UTF-8'><style>")
                append("body{font-family:sans-serif;padding:20px;color:#333}")
                append("h1{color:#2E7D32;border-bottom:3px solid #4CAF50;padding-bottom:8px}")
                append(".plant{border-left:4px solid #4CAF50;padding:8px 12px;margin:12px 0;background:#F9FBF9}")
                append(".name{font-size:18px;font-weight:bold;color:#2E7D32}")
                append(".latin{font-style:italic;color:#666;font-size:13px}")
                append(".info{color:#555;margin:3px 0;font-size:14px}")
                append(".note{color:#777;font-style:italic;font-size:13px}")
                append("</style></head><body>")
                append("<h1>🌱 Almanach du jardin — Semis de ${monthName()}</h1>")
                if (plants.isEmpty()) {
                    append("<p>Aucun semis conseillé pour ce mois.</p>")
                } else {
                    append("<p>${plants.size} plante${if(plants.size>1) "s" else ""} à semer en ${monthName()}</p>")
                    plants.forEach { p ->
                        append("<div class='plant'>")
                        append("<div class='name'>${p.emoji} ${p.name}</div>")
                        if (p.latinName.isNotEmpty()) append("<div class='latin'>${p.latinName}</div>")
                        append("<div class='info'>⏱ Occupation : ${p.occupationDays} jours &nbsp;|&nbsp; ↔️ Espacement : ${p.spacingCm} cm</div>")
                        append("<div class='info'>🌱 Germination : ${p.germinationDays} j à ${p.germinationTempMin}-${p.germinationTempMax}°C</div>")
                        append("<div class='info'>☀️ ${p.sunExposure} &nbsp;|&nbsp; 💧 ${p.waterNeeds}</div>")
                        if (p.notes.isNotEmpty()) append("<div class='note'>📝 ${p.notes}</div>")
                        append("</div>")
                    }
                }
                append("</body></html>")
            }

            val webView = android.webkit.WebView(requireContext())
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    if (!isAdded) return
                    val pm = requireActivity().getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                    pm.print("Almanach — Semis ${monthName()}", webView.createPrintDocumentAdapter("Semis ${monthName()}"),
                        android.print.PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
