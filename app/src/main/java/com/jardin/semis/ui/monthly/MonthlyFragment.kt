package com.jardin.semis.ui.monthly

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.print.PrintHelper
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.Plant
import com.jardin.semis.databinding.FragmentMonthlyBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MonthlyFragment : Fragment() {

    private var _binding: FragmentMonthlyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
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
            updateMonthDisplay()
            loadSowingsForMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonth = if (currentMonth == 12) 1 else currentMonth + 1
            updateMonthDisplay()
            loadSowingsForMonth()
        }

        binding.btnShare.setOnClickListener { shareContent() }
        binding.btnPrint.setOnClickListener { printContent() }
    }

    private fun updateMonthDisplay() {
        val monthName = LocalDate.of(2024, currentMonth, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }
        binding.tvMonthTitle.text = monthName
        binding.tvMonthSubtitle.text = "Semis conseillés pour $monthName"
    }

    private fun loadSowingsForMonth() {
        lifecycleScope.launch {
            val plants = viewModel.allPlants.first()
            val toSow = plants.filter { plant ->
                plant.sowingMonths.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .contains(currentMonth)
            }.sortedBy { it.name }

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

    private fun buildSowingText(plants: List<Plant>): String {
        return plants.joinToString("\n\n") { plant ->
            buildString {
                append("${plant.emoji} ${plant.name}")
                if (plant.latinName.isNotEmpty()) append(" (${plant.latinName})")
                append("\n")
                append("  ⏱ Occupation : ${plant.occupationDays} jours")
                append("\n")
                append("  ↔️ Espacement : ${plant.spacingCm} cm")
                append("\n")
                append("  🌱 Germination : ${plant.germinationDays} j à ${plant.germinationTempMin}-${plant.germinationTempMax}°C")
                append("\n")
                append("  ☀️ ${plant.sunExposure}  •  💧 Eau : ${plant.waterNeeds}")
                if (plant.notes.isNotEmpty()) {
                    append("\n  📝 ${plant.notes}")
                }
            }
        }
    }

    private fun shareContent() {
        val monthName = LocalDate.of(2024, currentMonth, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

        val text = buildString {
            append("🌱 Semis de $monthName — SemisJardin\n\n")
            append(binding.tvSowingList.text)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Semis de $monthName")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Partager la liste de semis"))
    }

    private fun printContent() {
        val monthName = LocalDate.of(2024, currentMonth, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

        // Construire le HTML pour l'impression
        val html = buildString {
            append("<html><head><style>")
            append("body { font-family: sans-serif; padding: 20px; }")
            append("h1 { color: #2E7D32; }")
            append("h2 { color: #388E3C; margin-top: 20px; }")
            append(".plant { border-left: 4px solid #4CAF50; padding-left: 12px; margin: 16px 0; }")
            append(".info { color: #555; margin: 4px 0; }")
            append(".note { color: #777; font-style: italic; }")
            append("</style></head><body>")
            append("<h1>🌱 Semis de $monthName</h1>")
            append("<p>Généré par SemisJardin</p>")

            lifecycleScope.launch {
                val plants = viewModel.allPlants.first().filter { plant ->
                    plant.sowingMonths.split(",").mapNotNull { it.trim().toIntOrNull() }.contains(currentMonth)
                }.sortedBy { it.name }

                plants.forEach { plant ->
                    append("<div class='plant'>")
                    append("<h2>${plant.emoji} ${plant.name}")
                    if (plant.latinName.isNotEmpty()) append(" <small><i>(${plant.latinName})</i></small>")
                    append("</h2>")
                    append("<p class='info'>⏱ Occupation sol : ${plant.occupationDays} jours</p>")
                    append("<p class='info'>↔️ Espacement : ${plant.spacingCm} cm</p>")
                    append("<p class='info'>🌱 Germination : ${plant.germinationDays} j à ${plant.germinationTempMin}-${plant.germinationTempMax}°C</p>")
                    append("<p class='info'>☀️ ${plant.sunExposure} &nbsp;|&nbsp; 💧 Eau : ${plant.waterNeeds}</p>")
                    if (plant.notes.isNotEmpty()) append("<p class='note'>📝 ${plant.notes}</p>")
                    append("</div>")
                }
            }
            append("</body></html>")
        }

        // Utiliser WebView pour imprimer
        val webView = android.webkit.WebView(requireContext())
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = requireActivity()
                    .getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val jobName = "SemisJardin — Semis $monthName"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = android.print.PrintAttributes.Builder().build()
                printManager.print(jobName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
