package com.jardin.semis.ui.growth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        ViewModelProvider(
            requireActivity(),
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

    private fun setupWeather() {
        binding.cityInputLayout.visibility = View.VISIBLE
        binding.btnFetchCityWeather.setOnClickListener {
            val city = binding.etCity.text?.toString()?.trim() ?: ""
            if (city.isNotEmpty()) {
                viewModel.fetchWeatherByCity(city)
                binding.cityInputLayout.visibility = View.GONE
            }
        }
        binding.btnRefreshWeather.setOnClickListener {
            binding.cityInputLayout.visibility = View.VISIBLE
        }
    }

    private fun observeWeather() {
        viewModel.weatherData.observe(viewLifecycleOwner) { weather ->
            if (weather == null) return@observe
            with(binding) {
                weatherCard.visibility = View.VISIBLE
                tvCity.text = "📍 ${weather.cityName}"
                tvTemperature.text = "${weather.temperature.toInt()}°C"
                tvDescription.text = weather.description
                tvHumidity.text = "💧 ${weather.humidity}%"
                tvWind.text = "💨 ${weather.windSpeed} m/s"
                tvTempMinMax.text = "↓${weather.tempMin.toInt()}° / ↑${weather.tempMax.toInt()}°"
                tvSowingAdvice.text = weather.sowingAdvice()
            }
        }
        viewModel.weatherLoading.observe(viewLifecycleOwner) { loading ->
            binding.weatherProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.weatherError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.cityInputLayout.visibility = View.VISIBLE
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
                if (sowings.isEmpty()) {
                    binding.emptyGrowth.visibility = View.VISIBLE
                    binding.growthContainer.visibility = View.GONE
                    return@collectLatest
                }
                binding.emptyGrowth.visibility = View.GONE
                binding.growthContainer.visibility = View.VISIBLE

                val upcoming = sowings.filter {
                    try {
                        val days = ChronoUnit.DAYS.between(today, LocalDate.parse(it.expectedHarvestDate, fmt))
                        days in 0..30
                    } catch (e: Exception) { false }
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
                val growing = sowings.count { it.status == SowingStatus.GROWING }
                binding.tvStatsGrowth.text =
                    "🌰 Semés : $sowed\n🌱 Levée : $germinated\n🌿 En croissance : $growing\nTotal actifs : ${sowings.size}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
