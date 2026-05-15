package com.elifoksas.earthquake.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    private lateinit var detailSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var compactDragStartY = 0f
    private var isDetailHeaderMagnitudeVisible = false
    private var lastDetailSheetSlideOffset = -1f

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
        private const val SELECTED_EARTHQUAKE_ZOOM = 10f
        private const val MIN_VISIBLE_MAP_HEIGHT_DP = 180
        private const val MAP_FOCUS_EXTRA_PADDING_DP = 24
        private const val LIST_SINGLE_EARTHQUAKE_ZOOM = 6f
        private const val LIST_MAP_EDGE_PADDING_DP = 40
        private const val COMPACT_EXPAND_DRAG_THRESHOLD_DP = 24
        private const val DETAIL_SHEET_PEEK_HEIGHT_DP = 92
        private const val DETAIL_HEADER_MAG_SLIDE_THRESHOLD = 0.92f
        private const val DETAIL_HEADER_MAG_ANIMATION_DURATION_MS = 260L
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
        setupDetailSheet()

        viewModel.earthquakes.observe(viewLifecycleOwner) { earthquakes ->
            allEarthquakes = earthquakes.result
            applyMagnitudeFilter()
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

    private fun setupDetailSheet() {
        detailSheetBehavior = BottomSheetBehavior.from(binding.earthquakeDetailsLayout)
        detailSheetBehavior.isHideable = true
        detailSheetBehavior.skipCollapsed = false
        detailSheetBehavior.peekHeight = dpToPx(DETAIL_SHEET_PEEK_HEIGHT_DP)
        detailSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        detailSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val earthquake = selectedEarthquake ?: return

                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        lastDetailSheetSlideOffset = 1f
                        setDetailHeaderMagnitudeVisible(visible = false, animate = true)
                        focusEarthquakeOnMap(earthquake)
                    }

                    BottomSheetBehavior.STATE_COLLAPSED,
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        lastDetailSheetSlideOffset = 0f
                        showCompactDetails()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val isSlidingDown = slideOffset < lastDetailSheetSlideOffset
                val isDraggingTowardCompactHeader =
                    isSlidingDown && slideOffset in 0f..DETAIL_HEADER_MAG_SLIDE_THRESHOLD

                setDetailHeaderMagnitudeVisible(
                    visible = isDraggingTowardCompactHeader,
                    animate = true
                )
                lastDetailSheetSlideOffset = slideOffset
            }
        })

        binding.backButton.setOnClickListener {
            closeEarthquakeDetails()
        }

        binding.compactBackButton.setOnClickListener {
            closeEarthquakeDetails()
        }

        binding.compactDetailsBar.setOnClickListener {
            expandCompactDetails()
        }

        setupCompactDetailsDrag()

        binding.shareButton.setOnClickListener {
            selectedEarthquake?.let { shareEarthquake(it) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCompactDetailsDrag() {
        val compactDragTargets = listOf(
            binding.compactDetailsBar,
            binding.compactDragHandle,
            binding.compactMagTV,
            binding.compactCountryTV,
            binding.compactSubtitleTV
        )

        compactDragTargets.forEach { view ->
            view.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        compactDragStartY = event.rawY
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dragDistance = compactDragStartY - event.rawY
                        if (dragDistance > dpToPx(COMPACT_EXPAND_DRAG_THRESHOLD_DP)) {
                            expandCompactDetails()
                            true
                        } else {
                            false
                        }
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        compactDragStartY = 0f
                        false
                    }

                    else -> false
                }
            }
        }
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
        val markerLocations = mutableListOf<LatLng>()

        filteredEarthquakes.forEach { earthquake ->
            val latitude = earthquake.geojson?.coordinates?.getOrNull(1) ?: return@forEach
            val longitude = earthquake.geojson?.coordinates?.getOrNull(0) ?: return@forEach
            val location = LatLng(latitude, longitude)
            markerLocations.add(location)

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(earthquake.title)
            )
            marker?.tag = earthquake
        }

        if (binding.recyclerView.visibility == View.VISIBLE) {
            focusEarthquakeListOnMap(markerLocations)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val earthquakeInfo = marker.tag as? Result
        earthquakeInfo?.let { showEarthquakeDetails(it) }
        return true
    }

    private fun handleItemClickDetails(item: Result) {
        showEarthquakeDetails(item)
    }

    private fun showEarthquakeDetails(item: Result) {
        selectedEarthquake = item

        val magnitude = formatMagnitude(item.mag)
        val title = item.title ?: "-"
        val subtitle = getLocationSubtitle(item)
        val elapsed = formatElapsedChip(item.date)

        binding.magTV.text = magnitude
        binding.heroMagnitudeTV.text = magnitude
        binding.depthTV.text = formatDepth(item.depth)
        binding.countryTV.text = title
        binding.dateTV.text = formatToDisplayDateOnly(item.date)
        binding.timeTV.text = formatToDisplayTime(item.date)
        binding.detailTimestampTV.text = formatToDisplayDate(item.date)
        binding.minutesPassedTV.text = elapsed
        binding.shakingStatusTV.text = getMagnitudeStatus(item.mag)
        binding.detailLocationSubtitleTV.text = subtitle
        binding.summaryLocationTV.text = getNearbyLocationText(item)
        binding.compactMagTV.text = magnitude
        binding.compactCountryTV.text = title
        binding.compactSubtitleTV.text = elapsed
        updateDistance(item)
        lastDetailSheetSlideOffset = -1f
        setDetailHeaderMagnitudeVisible(visible = false, animate = false)

        binding.recyclerView.visibility = View.GONE
        binding.compactDetailsBar.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE
        binding.earthquakeDetailsLayout.post {
            detailSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            focusEarthquakeOnMap(item)
        }
    }

    private fun closeEarthquakeDetails() {
        selectedEarthquake = null
        detailSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        binding.earthquakeDetailsLayout.visibility = View.INVISIBLE
        binding.compactDetailsBar.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        focusEarthquakeListOnMap(getFilteredEarthquakeLocations())
    }

    private fun showCompactDetails() {
        if (selectedEarthquake == null) return

        binding.earthquakeDetailsLayout.visibility = View.INVISIBLE
        binding.compactDetailsBar.visibility = View.VISIBLE
        selectedEarthquake?.let { focusEarthquakeOnMap(it) }
    }

    private fun expandCompactDetails() {
        val earthquake = selectedEarthquake ?: return

        binding.compactDetailsBar.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE
        lastDetailSheetSlideOffset = 0f
        setDetailHeaderMagnitudeVisible(visible = false, animate = false)
        binding.earthquakeDetailsLayout.post {
            detailSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            focusEarthquakeOnMap(earthquake)
        }
    }

    private fun setDetailHeaderMagnitudeVisible(visible: Boolean, animate: Boolean) {
        if (isDetailHeaderMagnitudeVisible == visible) return

        isDetailHeaderMagnitudeVisible = visible

        if (animate && binding.earthquakeDetailsLayout.isLaidOut) {
            val transition = TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(
                    ChangeBounds()
                        .addTarget(binding.magTV)
                        .addTarget(binding.countryTV)
                        .addTarget(binding.minutesPassedTV)
                )
                .addTransition(Fade(Fade.IN or Fade.OUT).addTarget(binding.magTV))
                .setDuration(DETAIL_HEADER_MAG_ANIMATION_DURATION_MS)

            transition.interpolator = DecelerateInterpolator()
            TransitionManager.beginDelayedTransition(binding.earthquakeDetailsLayout, transition)
        }

        ConstraintSet().apply {
            clone(binding.earthquakeDetailsLayout)
            setVisibility(binding.magTV.id, if (visible) View.VISIBLE else View.GONE)

            if (visible) {
                connect(
                    binding.countryTV.id,
                    ConstraintSet.START,
                    binding.magTV.id,
                    ConstraintSet.END,
                    dpToPx(10)
                )
                connect(
                    binding.minutesPassedTV.id,
                    ConstraintSet.START,
                    binding.magTV.id,
                    ConstraintSet.END,
                    dpToPx(10)
                )
            } else {
                connect(
                    binding.countryTV.id,
                    ConstraintSet.START,
                    binding.backButton.id,
                    ConstraintSet.END,
                    dpToPx(12)
                )
                connect(
                    binding.minutesPassedTV.id,
                    ConstraintSet.START,
                    binding.backButton.id,
                    ConstraintSet.END,
                    dpToPx(12)
                )
            }

            applyTo(binding.earthquakeDetailsLayout)
        }
    }

    private fun focusEarthquakeListOnMap(locations: List<LatLng>) {
        binding.recyclerView.post {
            updateMapPaddingForEarthquakeList()

            when (locations.size) {
                0 -> mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(TURKEY_LAT_LNG, 4f))
                1 -> mMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(locations.first(), LIST_SINGLE_EARTHQUAKE_ZOOM)
                )
                else -> {
                    val boundsBuilder = LatLngBounds.Builder()
                    locations.forEach { boundsBuilder.include(it) }
                    mMap?.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(),
                            dpToPx(LIST_MAP_EDGE_PADDING_DP)
                        )
                    )
                }
            }
        }
    }

    private fun focusEarthquakeOnMap(item: Result) {
        val coordinates = getEarthquakeCoordinates(item) ?: return

        binding.earthquakeDetailsLayout.post {
            updateMapPaddingForDetailSheet()
            mMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(coordinates, SELECTED_EARTHQUAKE_ZOOM)
            )
        }
    }

    private fun updateMapPaddingForDetailSheet() {
        val rootHeight = binding.root.height
        val minVisibleMapHeight = dpToPx(MIN_VISIBLE_MAP_HEIGHT_DP)
        val maxBottomPadding = (rootHeight - minVisibleMapHeight).coerceAtLeast(0)
        val visibleSheetHeight = when {
            binding.compactDetailsBar.visibility == View.VISIBLE -> binding.compactDetailsBar.height
            binding.earthquakeDetailsLayout.visibility == View.VISIBLE -> binding.earthquakeDetailsLayout.height
            else -> 0
        }
        val bottomPadding = if (visibleSheetHeight == 0) {
            0
        } else {
            (visibleSheetHeight + dpToPx(MAP_FOCUS_EXTRA_PADDING_DP)).coerceAtMost(maxBottomPadding)
        }

        mMap?.setPadding(0, 0, 0, bottomPadding)
    }

    private fun updateMapPaddingForEarthquakeList() {
        val rootHeight = binding.root.height
        val listTop = binding.recyclerView.top
        val bottomPadding = if (rootHeight > 0 && listTop > 0) {
            rootHeight - listTop + dpToPx(MAP_FOCUS_EXTRA_PADDING_DP)
        } else {
            0
        }

        mMap?.setPadding(0, 0, 0, bottomPadding.coerceAtLeast(0))
    }

    private fun getFilteredEarthquakeLocations(): List<LatLng> {
        return filteredEarthquakes.mapNotNull { getEarthquakeCoordinates(it) }
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun updateDistance(item: Result) {
        val earthquakeLocation = getEarthquakeCoordinates(item)
        val currentUserLocation = userLocation

        binding.distanceTV.text = if (earthquakeLocation != null && currentUserLocation != null) {
            val distance = calculateDistance(earthquakeLocation, currentUserLocation)
            "$distance km"
        } else {
            "-"
        }
    }

    private fun getEarthquakeCoordinates(item: Result): LatLng? {
        val latitude = item.geojson?.coordinates?.getOrNull(1)
        val longitude = item.geojson?.coordinates?.getOrNull(0)

        return if (latitude != null && longitude != null) {
            LatLng(latitude, longitude)
        } else {
            null
        }
    }

    private fun shareEarthquake(item: Result) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Earthquake details")
            putExtra(Intent.EXTRA_TEXT, buildShareText(item))
        }

        startActivity(Intent.createChooser(shareIntent, "Share earthquake details"))
    }

    private fun buildShareText(item: Result): String {
        val coordinates = getEarthquakeCoordinates(item)
        val lines = mutableListOf(
            "Earthquake details",
            "Location: ${item.title ?: "-"}",
            "Magnitude: ${formatMagnitude(item.mag)}",
            "Depth: ${formatDepth(item.depth)}",
            "Date: ${formatToDisplayDate(item.date)}"
        )

        val distance = binding.distanceTV.text?.toString().orEmpty()
        if (distance.isNotBlank() && distance != "-") {
            lines.add("Distance: $distance")
        }

        if (coordinates != null) {
            lines.add("Map: https://www.google.com/maps/search/?api=1&query=${coordinates.latitude},${coordinates.longitude}")
        }

        return lines.joinToString(separator = "\n")
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

    private fun formatMagnitude(magnitude: Double?): String {
        return magnitude?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
    }

    private fun formatDepth(depth: Double?): String {
        val formattedDepth = depth?.let {
            if (it % 1.0 == 0.0) {
                it.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", it)
            }
        } ?: "-"

        return if (formattedDepth == "-") formattedDepth else "$formattedDepth km"
    }

    private fun getMagnitudeStatus(magnitude: Double?): String {
        val value = magnitude ?: return "Magnitude unavailable"

        return when {
            value < 3.0 -> "Light shaking"
            value < 5.0 -> "Moderate shaking"
            value < 6.0 -> "Strong shaking"
            else -> "Severe shaking"
        }
    }

    private fun getLocationSubtitle(item: Result): String {
        val coordinates = getEarthquakeCoordinates(item) ?: return "Selected earthquake"

        return String.format(Locale.US, "%.3f, %.3f", coordinates.latitude, coordinates.longitude)
    }

    private fun getNearbyLocationText(item: Result): String {
        val closestCity = item.locationProperties?.closestCity?.name
        if (!closestCity.isNullOrBlank()) {
            return "Near $closestCity"
        }

        val epiCenter = item.locationProperties?.epiCenter?.name
        if (!epiCenter.isNullOrBlank()) {
            return "Near $epiCenter"
        }

        return getLocationSubtitle(item)
    }

    private fun formatElapsedChip(dateTime: String?): String {
        return when (val minutesPassed = calculateMinutesPassed(dateTime)) {
            "-" -> "-"
            "now" -> "now"
            else -> "$minutesPassed ago"
        }
    }

    private fun formatToDisplayDate(dateTime: String?): String {
        return formatDate(dateTime, "dd.MM.yyyy HH:mm", Locale.getDefault())
    }

    private fun formatToDisplayDateOnly(dateTime: String?): String {
        return formatDate(dateTime, "dd MMM", Locale.ENGLISH)
    }

    private fun formatToDisplayTime(dateTime: String?): String {
        return formatDate(dateTime, "HH:mm", Locale.getDefault())
    }

    private fun formatDate(dateTime: String?, pattern: String, locale: Locale): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val outputFormat = SimpleDateFormat(pattern, locale)
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
        if (minutes == 0L) {
            return "now"
        }

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
