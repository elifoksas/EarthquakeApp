package com.elifoksas.earthquake.ui.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.databinding.FragmentHomeBinding
import com.elifoksas.earthquake.ui.adapter.EarthquakeAdapter
import com.elifoksas.earthquake.ui.viewmodel.HomeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private lateinit var locationManager: LocationManager
    private lateinit var preferences: SharedPreferences
    private var userLocation: LatLng? = null
    private var allEarthquakes: List<Result> = emptyList()
    private var filteredEarthquakes: List<Result> = emptyList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val MAGNITUDE_FILTER_KEY = "MagnitudeFilter"
        private const val DEFAULT_MAGNITUDE_FILTER = "All magnitude"
        private val TURKEY_LAT_LNG = LatLng(39.9334, 32.8597)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.earthquakes.observe(viewLifecycleOwner) { earthquakes ->
            allEarthquakes = earthquakes.result
            applyMagnitudeFilter()
        }

        binding.backButton.setOnClickListener {
            binding.earthquakeDetailsLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(TURKEY_LAT_LNG, 4f))
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
        preferences.registerOnSharedPreferenceChangeListener(this)
        applyMagnitudeFilter()
    }

    override fun onStop() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                userLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                mMap?.isMyLocationEnabled = true
                Log.d("kullanici", userLocation!!.longitude.toString())
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnMarkerClickListener(this)
        getUserLocation()
        renderFilteredEarthquakes()
    }

    private fun applyMagnitudeFilter() {
        val minMagnitude = getSelectedMinimumMagnitude()
        filteredEarthquakes = allEarthquakes.filter { earthquake ->
            (earthquake.mag ?: 0.0) >= minMagnitude
        }

        if (_binding != null) {
            binding.recyclerView.adapter = EarthquakeAdapter(
                requireContext(),
                filteredEarthquakes,
                object : EarthquakeAdapter.OnItemClickListener {
                    override fun onItemClick(item: Result) {
                        handleItemClickDetails(item)
                    }
                }
            )
        }

        renderFilteredEarthquakes()
    }

    private fun getSelectedMinimumMagnitude(): Double {
        val selectedValue = preferences.getString(
            MAGNITUDE_FILTER_KEY,
            DEFAULT_MAGNITUDE_FILTER
        ) ?: DEFAULT_MAGNITUDE_FILTER

        return selectedValue.substringBefore("+").toDoubleOrNull() ?: 0.0
    }

    private fun renderFilteredEarthquakes() {
        val googleMap = mMap ?: return
        googleMap.clear()
        getUserLocation()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TURKEY_LAT_LNG, 4f))

        filteredEarthquakes.forEach { earthquake ->
            val latitude = earthquake.geojson?.coordinates?.getOrNull(1) ?: return@forEach
            val longitude = earthquake.geojson?.coordinates?.getOrNull(0) ?: return@forEach
            val location = LatLng(latitude, longitude)

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(earthquake.title)
            )
            marker?.tag = earthquake
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val earthquakeInfo = marker.tag as? Result
        earthquakeInfo?.let { showMarkerInfoWindow(it) }

        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 10f))
        return true
    }

    private fun showMarkerInfoWindow(earthquakeInfo: Result) {
        binding.magTV.text = earthquakeInfo.mag.toString()
        binding.depthTV.text = earthquakeInfo.depth.toString()
        binding.countryTV.text = earthquakeInfo.title.toString()
        val minutesPassed = calculateMinutesPassed(earthquakeInfo.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"

        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE
    }

    private fun handleItemClickDetails(item: Result) {
        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE

        binding.magTV.text = item.mag.toString()
        binding.depthTV.text = item.depth.toString()
        binding.countryTV.text = item.title.toString()
        val minutesPassed = calculateMinutesPassed(item.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"

        val latitude = item.geojson?.coordinates?.getOrNull(1) ?: 0.0
        val longitude = item.geojson?.coordinates?.getOrNull(0) ?: 0.0
        val location = LatLng(latitude, longitude)
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))

        val distance = calculateDistance(location, userLocation)
        binding.distanceTV.text = "$distance km"
    }

    private fun calculateDistance(earthquakeLocation: LatLng, userLocation: LatLng?): Long {
        val result = FloatArray(1)
        if (userLocation != null) {
            Location.distanceBetween(
                earthquakeLocation.latitude,
                earthquakeLocation.longitude,
                userLocation.latitude,
                userLocation.longitude,
                result
            )
        }

        return (result[0] / 1000).toLong()
    }

    private fun calculateMinutesPassed(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val currentDate = Date()
            val startDate = parseApiDate(dateTime) ?: return "-"
            val difference = currentDate.time - startDate.time
            val differenceInMinutes = kotlin.math.abs(difference / (60 * 1000))

            formatTimeDifference(differenceInMinutes)
        } catch (_: Exception) {
            "-"
        }
    }

    private fun parseApiDate(dateTime: String): Date? {
        val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss")
        patterns.forEach { pattern ->
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(dateTime)
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun formatTimeDifference(minutes: Long): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        val formattedString = StringBuilder()

        if (hours > 0) {
            formattedString.append("$hours h")
        }
        if (remainingMinutes > 0) {
            formattedString.append(" $remainingMinutes m")
        }

        return formattedString.toString().trim()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MAGNITUDE_FILTER_KEY) {
            applyMagnitudeFilter()
        }
    }

    override fun onDestroyView() {
        mMap?.setOnMarkerClickListener(null)
        mMap = null
        _binding = null
        super.onDestroyView()
    }
}
