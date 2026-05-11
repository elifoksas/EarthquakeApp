package com.elifoksas.earthquake.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.Result
import com.elifoksas.earthquake.databinding.FragmentHomeBinding
import com.elifoksas.earthquake.ui.adapter.EarthquakeAdapter
import com.elifoksas.earthquake.ui.preference.MagnitudePreference
import com.elifoksas.earthquake.ui.viewmodel.HomeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var preferences: SharedPreferences
    private var userLocation: LatLng? = null
    private var allEarthquakes: List<Result> = emptyList()
    private var filteredEarthquakes: List<Result> = emptyList()
    private var selectedEarthquake: Result? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocationFeatures()
        }
    }

    companion object {
        private const val MAPS_TYPE_KEY = "MapsType"
        private const val DEFAULT_MAPS_TYPE = "normal"
        private val TURKEY_LAT_LNG = LatLng(39.9334, 32.8597)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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

    @SuppressLint("MissingPermission")
    private fun enableMyLocationFeatures() {
        if (hasLocationPermission()) {
            mMap?.isMyLocationEnabled = true
            mMap?.uiSettings?.isMyLocationButtonEnabled = true

            locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                userLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                Log.d("kullanici", userLocation!!.longitude.toString())
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    selectedEarthquake?.let { updateDistance(it) }
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnMarkerClickListener(this)
        applyMapType()
        enableMyLocationFeatures()
        renderFilteredEarthquakes()
    }

    private fun applyMapType() {
        val googleMap = mMap ?: return
        googleMap.mapType = getSelectedMapType()
    }

    private fun getSelectedMapType(): Int {
        return when (preferences.getString(MAPS_TYPE_KEY, DEFAULT_MAPS_TYPE)?.lowercase(Locale.US)) {
            "terrain", "2" -> GoogleMap.MAP_TYPE_TERRAIN
            "satellite", "3" -> GoogleMap.MAP_TYPE_SATELLITE
            "hybrid", "4" -> GoogleMap.MAP_TYPE_HYBRID
            else -> GoogleMap.MAP_TYPE_NORMAL
        }
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
        val storedMagnitude = preferences.getInt(
            MagnitudePreference.KEY,
            MagnitudePreference.DEFAULT_STORED_VALUE
        )

        return MagnitudePreference.toMagnitude(storedMagnitude)
    }

    private fun renderFilteredEarthquakes() {
        val googleMap = mMap ?: return
        googleMap.clear()
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
        selectedEarthquake = earthquakeInfo
        binding.magTV.text = earthquakeInfo.mag.toString()
        binding.depthTV.text = earthquakeInfo.depth.toString()
        binding.countryTV.text = earthquakeInfo.title.toString()
        binding.dateTV.text = formatToDisplayDate(earthquakeInfo.date)
        val minutesPassed = calculateMinutesPassed(earthquakeInfo.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"
        updateDistance(earthquakeInfo)

        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE
    }

    private fun handleItemClickDetails(item: Result) {
        selectedEarthquake = item
        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE

        binding.magTV.text = item.mag.toString()
        binding.depthTV.text = item.depth.toString()
        binding.countryTV.text = item.title.toString()
        binding.dateTV.text = formatToDisplayDate(item.date)
        val minutesPassed = calculateMinutesPassed(item.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"
        updateDistance(item)

        val latitude = item.geojson?.coordinates?.getOrNull(1) ?: 0.0
        val longitude = item.geojson?.coordinates?.getOrNull(0) ?: 0.0
        val location = LatLng(latitude, longitude)
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }

    private fun updateDistance(item: Result) {
        val latitude = item.geojson?.coordinates?.getOrNull(1)
        val longitude = item.geojson?.coordinates?.getOrNull(0)
        val currentUserLocation = userLocation

        binding.distanceTV.text = if (latitude != null && longitude != null && currentUserLocation != null) {
            val distance = calculateDistance(LatLng(latitude, longitude), currentUserLocation)
            "$distance km"
        } else {
            "-"
        }
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

    private fun formatToDisplayDate(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = parseApiDate(dateTime) ?: return "-"
            outputFormat.format(date)
        } catch (_: Exception) {
            "-"
        }
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
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy.MM.dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
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
        if (key == MagnitudePreference.KEY) {
            applyMagnitudeFilter()
        }
        if (key == MAPS_TYPE_KEY) {
            applyMapType()
        }
    }

    override fun onDestroyView() {
        mMap?.setOnMarkerClickListener(null)
        mMap = null
        _binding = null
        super.onDestroyView()
    }
}
