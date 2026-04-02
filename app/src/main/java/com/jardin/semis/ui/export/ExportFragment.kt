package com.jardin.semis.ui.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisApplication
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.NaturalEvent
import com.jardin.semis.data.model.Plant
import com.jardin.semis.data.model.Sowing
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.FragmentExportBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    // Launcher pour sélectionner un fichier JSON à importer
    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> importFromJson(uri) }
        }
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
        binding.btnImport.setOnClickListener { openFilePicker() }
        binding.btnPrintAll.setOnClickListener { printAllEvents() }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "text/x-json"))
        }
        importLauncher.launch(Intent.createChooser(intent, "Choisir la sauvegarde JSON"))
    }

    private fun importFromJson(uri: Uri) {
        lifecycleScope.launch {
            try {
                val content = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val json = JSONObject(content)
                val repo = (requireActivity().application as SemisApplication).repository

                // Importer les plantes
                var plantsImported = 0
                json.optJSONArray("plantes")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val plant = Plant(
                            name = obj.optString("nom", ""),
                            latinName = obj.optString("nomLatin", ""),
                            category = obj.optString("categorie", "Légume"),
                            emoji = obj.optString("emoji", "🌱"),
                            sowingMonths = obj.optString("moisSemis", ""),
                            occupationDays = obj.optInt("occupation", 90),
                            spacingCm = obj.optInt("espacement", 30),
                            sunExposure = obj.optString("exposition", "Plein soleil"),
                            waterNeeds = obj.optString("eau", "Moyen"),
                            germinationDays = obj.optInt("germination", 10),
                            notes = obj.optString("notes", ""),
                            isDefault = false
                        )
                        if (plant.name.isNotEmpty()) { repo.insertPlant(plant); plantsImported++ }
                    }
                }

                // Importer les observations
                var eventsImported = 0
                json.optJSONArray("observations")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val event = NaturalEvent(
                            eventDate = obj.optString("date", LocalDate.now().toString()),
                            title = obj.optString("titre", ""),
                            category = obj.optString("categorie", "Autre"),
                            emoji = obj.optString("emoji", "📝"),
                            description = obj.optString("description", ""),
                            location = obj.optString("lieu", "")
                        )
                        if (event.title.isNotEmpty()) { repo.addNaturalEvent(event); eventsImported++ }
                    }
                }

                val msg = buildString {
                    append("✅ Import réussi !")
                    if (plantsImported > 0) append(" $plantsImported plante(s)")
                    if (eventsImported > 0) append(", $eventsImported observation(s)")
                    if (plantsImported == 0 && eventsImported == 0) append(" — Aucune donnée importée (vérifiez le fichier)")
                }
                if (_binding != null) {
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                }
            } catch (e: Exception) {
                showMessage("❌ Erreur d'import : ${e.message}")
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

                val root = JSONObject()
                root.put("exportDate", date)
                root.put("appVersion", "Almanach du jardin")

                val plantsArr = JSONArray()
                plants.forEach { p ->
                    plantsArr.put(JSONObject().apply {
                        put("nom", p.name); put("nomLatin", p.latinName)
                        put("categorie", p.category); put("emoji", p.emoji)
                        put("moisSemis", p.sowingMonths); put("occupation", p.occupationDays)
                        put("espacement", p.spacingCm); put("exposition", p.sunExposure)
                        put("eau", p.waterNeeds); put("germination", p.germinationDays)
                        put("notes", p.notes)
                    })
                }
                root.put("plantes", plantsArr)

                val semisArr = JSONArray()
                sowings.forEach { s ->
                    semisArr.put(JSONObject().apply {
                        put("plante", s.plantName); put("date", s.sowingDate)
                        put("recolte", s.expectedHarvestDate); put("emplacement", s.location)
                        put("quantite", s.quantity); put("statut", s.status.name)
                        put("notes", s.notes)
                    })
                }
                root.put("semis", semisArr)

                val obsArr = JSONArray()
                events.forEach { e ->
                    obsArr.put(JSONObject().apply {
                        put("date", e.eventDate); put("titre", e.title)
                        put("categorie", e.category); put("emoji", e.emoji)
                        put("description", e.description); put("lieu", e.location)
                    })
                }
                root.put("observations", obsArr)

                shareFile(root.toString(2), "almanach_jardin_$date.json", "application/json")
            } catch (e: Exception) { showMessage("Erreur : ${e.message}") }
        }
    }

    private fun exportSowingsCSV() {
        lifecycleScope.launch {
            try {
                val sowings = viewModel.allSowingsWithPlant.first()
                val sb = StringBuilder("Plante,Date semis,Récolte prévue,Emplacement,Quantité,Statut,Notes\n")
                sowings.forEach { s ->
                    sb.appendLine("\"${s.plantName}\",\"${s.sowingDate}\",\"${s.expectedHarvestDate}\",\"${s.location}\",${s.quantity},\"${s.status}\",\"${s.notes}\"")
                }
                shareFile(sb.toString(), "semis.csv", "text/csv")
            } catch (e: Exception) { showMessage("Erreur : ${e.message}") }
        }
    }

    private fun exportPlantsCSV() {
        lifecycleScope.launch {
            try {
                val plants = viewModel.allPlants.first()
                val sb = StringBuilder("Nom,Nom latin,Catégorie,Mois semis,Occupation sol,Espacement,Exposition,Eau,Germination,Notes\n")
                plants.forEach { p ->
                    sb.appendLine("\"${p.name}\",\"${p.latinName}\",\"${p.category}\",\"${p.sowingMonths}\",${p.occupationDays},${p.spacingCm},\"${p.sunExposure}\",\"${p.waterNeeds}\",${p.germinationDays},\"${p.notes}\"")
                }
                shareFile(sb.toString(), "bibliotheque_plantes.csv", "text/csv")
            } catch (e: Exception) { showMessage("Erreur : ${e.message}") }
        }
    }

    private fun exportEventsCSV() {
        lifecycleScope.launch {
            try {
                val events = viewModel.allNaturalEvents.first()
                val sb = StringBuilder("Date,Titre,Catégorie,Description,Lieu\n")
                events.forEach { e ->
                    sb.appendLine("\"${e.eventDate}\",\"${e.title}\",\"${e.category}\",\"${e.description}\",\"${e.location}\"")
                }
                shareFile(sb.toString(), "journal_jardin.csv", "text/csv")
            } catch (e: Exception) { showMessage("Erreur : ${e.message}") }
        }
    }

    private fun printAllEvents() {
        lifecycleScope.launch {
            try {
                val sowings = viewModel.allSowingsWithPlant.first()
                val events = viewModel.allNaturalEvents.first()
                val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))

                val html = buildString {
                    append("<html><head><meta charset='UTF-8'><style>")
                    append("body{font-family:sans-serif;padding:20px;color:#333}")
                    append("h1{color:#2E7D32;border-bottom:2px solid #4CAF50;padding-bottom:8px}")
                    append("h2{color:#388E3C;margin-top:24px}")
                    append("table{width:100%;border-collapse:collapse;margin:12px 0}")
                    append("th{background:#E8F5E9;color:#2E7D32;padding:8px;border:1px solid #C8E6C9}")
                    append("td{padding:6px 8px;border:1px solid #E0E0E0}")
                    append("tr:nth-child(even){background:#F9FBF9}")
                    append("</style></head><body>")
                    append("<h1>🌱 Almanach du jardin — Rapport complet</h1>")
                    append("<p>Généré le $dateStr</p>")
                    if (sowings.isNotEmpty()) {
                        append("<h2>📅 Mes semis (${sowings.size})</h2>")
                        append("<table><tr><th>Plante</th><th>Date semis</th><th>Récolte prévue</th><th>Emplacement</th><th>Statut</th></tr>")
                        sowings.forEach { s ->
                            val st = when(s.status.name) { "SOWED"->"Semé";"GERMINATED"->"Levée";"GROWING"->"Croissance";"HARVESTED"->"Récolté";"FAILED"->"Échec";else->s.status.name }
                            append("<tr><td>${s.plantEmoji} ${s.plantName}</td><td>${s.sowingDate}</td><td>${s.expectedHarvestDate}</td><td>${s.location}</td><td>$st</td></tr>")
                        }
                        append("</table>")
                    }
                    if (events.isNotEmpty()) {
                        append("<h2>📔 Journal du jardin (${events.size} observations)</h2>")
                        append("<table><tr><th>Date</th><th>Observation</th><th>Catégorie</th><th>Description</th></tr>")
                        events.forEach { e -> append("<tr><td>${e.eventDate}</td><td>${e.emoji} ${e.title}</td><td>${e.category}</td><td>${e.description}</td></tr>") }
                        append("</table>")
                    }
                    append("<p style='color:#999;font-size:11px;text-align:center'>Almanach du jardin • $dateStr</p>")
                    append("</body></html>")
                }

                if (!isAdded) return@launch
                val webView = android.webkit.WebView(requireContext())
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(v: android.webkit.WebView?, url: String?) {
                        if (!isAdded) return
                        val pm = requireActivity().getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        pm.print("Almanach du jardin", webView.createPrintDocumentAdapter("Rapport"),
                            android.print.PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            } catch (e: Exception) { showMessage("Erreur : ${e.message}") }
        }
    }

    private fun shareFile(content: String, filename: String, mimeType: String) {
        try {
            val file = File(requireContext().cacheDir, filename)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType; putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Exporter via..."))
        } catch (e: Exception) { showMessage("Erreur partage : ${e.message}") }
    }

    private fun showMessage(msg: String) {
        if (_binding != null) Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
