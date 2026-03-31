package com.jardin.semis.ui.export

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisViewModel
import com.jardin.semis.databinding.FragmentExportBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExportSowings.setOnClickListener { exportSowingsCSV() }
        binding.btnExportPlants.setOnClickListener { exportPlantsCSV() }
        binding.btnExportEvents.setOnClickListener { exportEventsCSV() }
        binding.btnExportAll.setOnClickListener { exportAllJSON() }
        binding.btnPrintAll.setOnClickListener { printAllEvents() }
    }

    private fun exportSowingsCSV() {
        lifecycleScope.launch {
            try {
                val sowings = viewModel.allSowingsWithPlant.first()
                val sb = StringBuilder()
                sb.appendLine("Plante,Date semis,Récolte prévue,Emplacement,Quantité,Statut,Notes")
                sowings.forEach { s ->
                    sb.appendLine("\"${s.plantName}\",\"${s.sowingDate}\",\"${s.expectedHarvestDate}\",\"${s.location}\",${s.quantity},\"${s.status}\",\"${s.notes}\"")
                }
                shareFile(sb.toString(), "semis_jardin.csv", "text/csv")
            } catch (e: Exception) {
                showError("Erreur export semis : ${e.message}")
            }
        }
    }

    private fun exportPlantsCSV() {
        lifecycleScope.launch {
            try {
                val plants = viewModel.allPlants.first()
                val sb = StringBuilder()
                sb.appendLine("Nom,Nom latin,Catégorie,Mois semis,Occupation sol (j),Espacement (cm),Exposition,Eau,Germination (j),Notes")
                plants.forEach { p ->
                    sb.appendLine("\"${p.name}\",\"${p.latinName}\",\"${p.category}\",\"${p.sowingMonths}\",${p.occupationDays},${p.spacingCm},\"${p.sunExposure}\",\"${p.waterNeeds}\",${p.germinationDays},\"${p.notes}\"")
                }
                shareFile(sb.toString(), "bibliotheque_plantes.csv", "text/csv")
            } catch (e: Exception) {
                showError("Erreur export plantes : ${e.message}")
            }
        }
    }

    private fun exportEventsCSV() {
        lifecycleScope.launch {
            try {
                val events = viewModel.allNaturalEvents.first()
                val sb = StringBuilder()
                sb.appendLine("Date,Titre,Catégorie,Description,Lieu")
                events.forEach { e ->
                    sb.appendLine("\"${e.eventDate}\",\"${e.title}\",\"${e.category}\",\"${e.description}\",\"${e.location}\"")
                }
                shareFile(sb.toString(), "journal_jardin.csv", "text/csv")
            } catch (e: Exception) {
                showError("Erreur export journal : ${e.message}")
            }
        }
    }

    private fun exportAllJSON() {
        lifecycleScope.launch {
            try {
                val plants = viewModel.allPlants.first()
                val sowings = viewModel.allSowingsWithPlant.first()
                val events = viewModel.allNaturalEvents.first()
                val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                val json = buildString {
                    appendLine("{")
                    appendLine("  \"exportDate\": \"$date\",")
                    appendLine("  \"plantes\": [")
                    plants.forEachIndexed { i, p ->
                        appendLine("    {\"nom\": \"${p.name}\", \"categorie\": \"${p.category}\", \"occupation\": ${p.occupationDays}}${if (i < plants.lastIndex) "," else ""}")
                    }
                    appendLine("  ],")
                    appendLine("  \"semis\": [")
                    sowings.forEachIndexed { i, s ->
                        appendLine("    {\"plante\": \"${s.plantName}\", \"date\": \"${s.sowingDate}\", \"recolte\": \"${s.expectedHarvestDate}\", \"statut\": \"${s.status}\"}${if (i < sowings.lastIndex) "," else ""}")
                    }
                    appendLine("  ],")
                    appendLine("  \"observations\": [")
                    events.forEachIndexed { i, e ->
                        appendLine("    {\"date\": \"${e.eventDate}\", \"titre\": \"${e.title}\", \"categorie\": \"${e.category}\"}${if (i < events.lastIndex) "," else ""}")
                    }
                    appendLine("  ]")
                    appendLine("}")
                }
                shareFile(json, "sauvegarde_jardin_$date.json", "application/json")
            } catch (e: Exception) {
                showError("Erreur export JSON : ${e.message}")
            }
        }
    }

    private fun printAllEvents() {
        lifecycleScope.launch {
            try {
                val sowings = viewModel.allSowingsWithPlant.first()
                val events = viewModel.allNaturalEvents.first()
                val date = LocalDate.now()
                val dateStr = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))

                val html = buildString {
                    append("<html><head><meta charset='UTF-8'><style>")
                    append("body{font-family:sans-serif;padding:20px;color:#333}")
                    append("h1{color:#2E7D32;border-bottom:2px solid #4CAF50;padding-bottom:8px}")
                    append("h2{color:#388E3C;margin-top:24px}")
                    append("table{width:100%;border-collapse:collapse;margin:12px 0}")
                    append("th{background:#E8F5E9;color:#2E7D32;padding:8px;text-align:left;border:1px solid #C8E6C9}")
                    append("td{padding:6px 8px;border:1px solid #E0E0E0}")
                    append("tr:nth-child(even){background:#F9FBF9}")
                    append(".footer{margin-top:40px;font-size:11px;color:#999;text-align:center}")
                    append("</style></head><body>")
                    append("<h1>🌱 SemisJardin — Rapport complet</h1>")
                    append("<p>Généré le $dateStr</p>")

                    if (sowings.isNotEmpty()) {
                        append("<h2>📅 Mes semis (${sowings.size})</h2>")
                        append("<table><tr><th>Plante</th><th>Date semis</th><th>Récolte prévue</th><th>Emplacement</th><th>Statut</th></tr>")
                        sowings.forEach { s ->
                            val status = when (s.status.name) {
                                "SOWED" -> "Semé"; "GERMINATED" -> "Levée"; "GROWING" -> "Croissance"
                                "HARVESTED" -> "Récolté"; "FAILED" -> "Échec"; else -> s.status.name
                            }
                            append("<tr><td>${s.plantEmoji} ${s.plantName}</td><td>${s.sowingDate}</td><td>${s.expectedHarvestDate}</td><td>${s.location}</td><td>$status</td></tr>")
                        }
                        append("</table>")
                    }

                    if (events.isNotEmpty()) {
                        append("<h2>📔 Journal du jardin (${events.size} observations)</h2>")
                        append("<table><tr><th>Date</th><th>Observation</th><th>Catégorie</th><th>Description</th></tr>")
                        events.forEach { e ->
                            append("<tr><td>${e.eventDate}</td><td>${e.emoji} ${e.title}</td><td>${e.category}</td><td>${e.description}</td></tr>")
                        }
                        append("</table>")
                    }

                    append("<p class='footer'>SemisJardin • Exporté le $dateStr</p>")
                    append("</body></html>")
                }

                val webView = android.webkit.WebView(requireContext())
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        val printManager = requireActivity()
                            .getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val jobName = "SemisJardin — Rapport $dateStr"
                        val adapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)

            } catch (e: Exception) {
                showError("Erreur impression : ${e.message}")
            }
        }
    }

    private fun shareFile(content: String, filename: String, mimeType: String) {
        try {
            val file = File(requireContext().cacheDir, filename)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Exporter via..."))
        } catch (e: Exception) {
            showError("Erreur partage : ${e.message}")
        }
    }

    private fun showError(msg: String) {
        if (_binding != null) Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
