package com.jardin.semis.ui.growth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jardin.semis.SemisViewModel
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.databinding.FragmentGrowthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class GrowthFragment : Fragment() {

    private var _binding: FragmentGrowthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SemisViewModel by lazy {
        ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SemisViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGrowthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWeather()
        observeWeather()
        observeGrowthTimeline()
    }

    private fun fetchWeather() {
        val city = binding.etCity.text?.toString()?.trim() ?: ""
        if (city.isEmpty()) { binding.cityInputLayout.error = "Entrez le nom de votre ville"; return }
        binding.cityInputLayout.error = null
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCity.windowToken, 0)
        viewModel.fetchWeatherByCity(city)
    }

    private fun setupWeather() {
        binding.btnFetchCityWeather.setOnClickListener { fetchWeather() }
        binding.etCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { fetchWeather(); true } else false
        }
        binding.btnRefreshWeather.setOnClickListener {
            binding.weatherCard.visibility = View.GONE
            binding.tvDataSource.visibility = View.GONE
            binding.cityInputLayout.visibility = View.VISIBLE
            binding.btnFetchCityWeather.visibility = View.VISIBLE
            binding.etCity.requestFocus()
        }
    }

    private fun observeWeather() {
        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            if (weather == null) return@observe
            binding.weatherCard.visibility = View.VISIBLE
            binding.tvDataSource.visibility = View.VISIBLE
            binding.cityInputLayout.visibility = View.GONE
            binding.btnFetchCityWeather.visibility = View.GONE

            with(binding) {
                tvCity.text = "📍 ${weather.cityName}"
                tvWeatherIcon.text = weather.icon
                tvTemperature.text = "${weather.temperature.toInt()}°C"
                tvDescription.text = weather.description
                tvHumidity.text = "💧 ${weather.humidity}%"
                tvWind.text = "💨 ${String.format("%.0f", weather.windSpeed)} km/h"
                tvTempMinMax.text = "↓${weather.tempMin.toInt()}° ↑${weather.tempMax.toInt()}°"
                tvPrecipitation.text = "🌧️ ${String.format("%.1f", weather.precipitation)} mm"
                tvEt0.text = "🌱 ET₀ ${String.format("%.1f", weather.evapotranspiration)} mm/j"
                tvSowingAdvice.text = weather.sowingAdvice()
                tvIrrigationAdvice.text = weather.irrigationAdvice()
            }
        }

        viewModel.weatherLoading.observe(viewLifecycleOwner) { loading ->
            if (_binding == null) return@observe
            binding.weatherProgress.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnFetchCityWeather.isEnabled = !loading
        }

        viewModel.weatherError.observe(viewLifecycleOwner) { error ->
            if (error != null && _binding != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeGrowthTimeline() {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val displayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        val today = LocalDate.now()

        lifecycleScope.launch {
            viewModel.activeSowings.collectLatest { sowings ->
                if (_binding == null) return@collectLatest
                if (sowings.isEmpty()) {
                    binding.emptyGrowth.visibility = View.VISIBLE
                    binding.growthContainer.visibility = View.GONE
                    binding.tvUpcomingTitle.visibility = View.GONE
                    binding.tvUpcomingHarvests.visibility = View.GONE
                    return@collectLatest
                }
                binding.emptyGrowth.visibility = View.GONE
                binding.growthContainer.visibility = View.VISIBLE

                val upcoming = sowings.filter {
                    try { ChronoUnit.DAYS.between(today, LocalDate.parse(it.expectedHarvestDate, fmt)) in 0..30 }
                    catch (e: Exception) { false }
                }.sortedBy { it.expectedHarvestDate }

                if (upcoming.isNotEmpty()) {
                    binding.tvUpcomingTitle.visibility = View.VISIBLE
                    binding.tvUpcomingHarvests.visibility = View.VISIBLE
                    binding.tvUpcomingHarvests.text = upcoming.joinToString("\n") {
                        val harvest = LocalDate.parse(it.expectedHarvestDate, fmt)
                        val days = ChronoUnit.DAYS.between(today, harvest)
                        "🌱 Semis #${it.id} — dans $days j (${harvest.format(displayFmt)})"
                    }
                } else {
                    binding.tvUpcomingTitle.visibility = View.GONE
                    binding.tvUpcomingHarvests.visibility = View.GONE
                }

                val sowed = sowings.count { it.status == SowingStatus.SOWED }
                val germinated = sowings.count { it.status == SowingStatus.GERMINATED }
                val transplanted = sowings.count { it.status == SowingStatus.TRANSPLANTED }
                val growing = sowings.count { it.status == SowingStatus.GROWING }
                binding.tvStatsGrowth.text = buildString {
                    append("🌰 Semés : $sowed\n")
                    append("🌱 Levée : $germinated\n")
                    if (transplanted > 0) append("🪴 Repiqués : $transplanted\n")
                    append("🌿 En croissance : $growing\n")
                    append("Total actifs : ${sowings.size}")
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
