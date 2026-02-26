package com.techgeeksclub.earthquake.ui.fragment

import android.Manifest
import android.content.Context
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.techgeeksclub.earthquake.data.entity.Result
import com.techgeeksclub.earthquake.databinding.FragmentHomeBinding
import com.techgeeksclub.earthquake.ui.adapter.EarthquakeAdapter
import com.techgeeksclub.earthquake.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding
    private var mMap: GoogleMap? = null
    private lateinit var locationManager: LocationManager
    private  val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var userLocation: LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tempViewModel : HomeViewModel by viewModels()
        viewModel = tempViewModel
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager

        getUserLocation()


        viewModel.earthquakes.observe(viewLifecycleOwner){
            val adapter = EarthquakeAdapter(requireContext(),it,object :
                EarthquakeAdapter.OnItemClickListener {
                override fun onItemClick(item: Result) {
                    // Details of the clicked item are displayed here.
                   handleItemClickDetails(item)
                }
                })
            binding.recyclerView.adapter = adapter
        }

        binding.backButton.setOnClickListener {
            binding.earthquakeDetailsLayout.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE

            //Focuses back on the map when the back button is pressed
            mMap.let {
                val turkeyLatLng = LatLng(39.9334, 32.8597)
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(turkeyLatLng, 4f))
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(com.techgeeksclub.earthquake.R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun getUserLocation(){
        // Konum izni kontrolü
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izni varsa konumu al ve haritada göster
            locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null){
                userLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!,15f))
                Log.d("kullanici" , userLocation!!.longitude.toString())
            }
            mMap?.isMyLocationEnabled = true
        } else {
            // Konum izni yoksa izin iste
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

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        mMap?.setOnMarkerClickListener(this)

        viewModel.earthquakes.observe(viewLifecycleOwner) { earthquakesList ->
            mMap?.clear()

            getUserLocation()

            val turkeyLatLng = LatLng(39.9334, 32.8597)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(turkeyLatLng, 4f))

            //Adding pins for each earthquake
            earthquakesList.result.forEach { earthquake ->
                val coordinates = earthquake.geojson?.coordinates
                val latitude = coordinates?.get(1)
                val longitude = coordinates?.get(0)

                val location = LatLng(latitude!!, longitude!!)
                val marker = mMap?.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(earthquake.title)
                )
                marker?.tag = earthquake
            }
        }
    }
    override fun onMarkerClick(marker: Marker): Boolean {
        // Get information about the clicked marker
        val earthquakeInfo = marker.tag as? Result
        earthquakeInfo?.let { showMarkerInfoWindow(it) }

        // Focus the map on the location of the clicked marker
        marker.position.let { location ->
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
        }

        return true
    }

    private fun showMarkerInfoWindow(earthquakeInfo: Result) {
        // Update the info window using the marker's information
        binding.magTV.text = earthquakeInfo.mag.toString()
        binding.depthTV.text = earthquakeInfo.depth.toString()
        binding.countryTV.text = earthquakeInfo.title.toString()
        val minutesPassed = calculateMinutesPassed(earthquakeInfo.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"

        // Show info window
        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE
    }

    private fun handleItemClickDetails(item:Result){
        binding.recyclerView.visibility = View.GONE
        binding.earthquakeDetailsLayout.visibility = View.VISIBLE

        binding.magTV.text = item.mag.toString()
        binding.depthTV.text = item.depth.toString()
        binding.countryTV.text = item.title.toString()
        val minutesPassed = calculateMinutesPassed(item.date)
        binding.minutesPassedTV.text = if (minutesPassed == "-") "-" else "$minutesPassed ago"

        // Focus the map on the location of the clicked item
        val latitude = item.geojson?.coordinates?.get(1) ?: 0.0
        val longitude = item.geojson?.coordinates?.get(0) ?: 0.0
        val location = LatLng(latitude, longitude)
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))

        val distance = calculateDistance(location,userLocation)

        binding.distanceTV.text = "$distance km"

    }

    private fun calculateDistance(earthquakeLocation: LatLng, userLocation: LatLng?): Long {
        val result = FloatArray(1)
        if (userLocation != null) {
            Location.distanceBetween(
                earthquakeLocation.latitude, earthquakeLocation.longitude,
                userLocation.latitude, userLocation.longitude, result
            )
        }

        return (result[0] / 1000).toLong() // m to km
    }

    private fun calculateMinutesPassed(dateTime: String?): String {
        if (dateTime.isNullOrBlank() || dateTime.equals("null", ignoreCase = true)) {
            return "-"
        }

        return try {
            val inputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
            val currentDate = Date()
            val startDate = inputFormat.parse(dateTime) ?: return "-"
            val difference = currentDate.time - startDate.time
            val differenceInMinutes = Math.abs(difference / (60 * 1000))

            formatTimeDifference(differenceInMinutes)
        } catch (e: Exception) {
            "-"
        }
    }

    private fun formatTimeDifference(minutes : Long) : String {

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

}
