package com.elifoksas.earthquake

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Typeface
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.elifoksas.earthquake.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setBottomNavigation()
    }

    private fun setBottomNavigation(){
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.navEarthquakes.setOnClickListener { navigateToTopLevel(R.id.homeFragment) }
        binding.navEmergency.setOnClickListener { navigateToTopLevel(R.id.emergencyFragment) }
        binding.navSettings.setOnClickListener { navigateToTopLevel(R.id.settingsFragment) }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val selectedDestination = when (destination.id) {
                R.id.emergencyFragment,
                R.id.whistleFragment,
                R.id.informationFragment,
                R.id.emergencyNumbersFragment -> R.id.emergencyFragment
                R.id.settingsFragment -> R.id.settingsFragment
                else -> R.id.homeFragment
            }
            updateBottomNavigation(selectedDestination)
        }
    }

    private fun navigateToTopLevel(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        navController.navigate(
            destinationId,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(R.id.homeFragment) {
                    saveState = true
                }
            }
        )
    }

    private fun updateBottomNavigation(destinationId: Int) {
        val isEarthquakesSelected = destinationId == R.id.homeFragment
        val isEmergencySelected = destinationId == R.id.emergencyFragment
        val isSettingsSelected = destinationId == R.id.settingsFragment

        binding.navEarthquakes.isSelected = isEarthquakesSelected
        binding.navEmergency.isSelected = isEmergencySelected
        binding.navSettings.isSelected = isSettingsSelected

        binding.navEarthquakesLabel.setTypeface(null, if (isEarthquakesSelected) Typeface.BOLD else Typeface.NORMAL)
        binding.navEmergencyLabel.setTypeface(null, if (isEmergencySelected) Typeface.BOLD else Typeface.NORMAL)
        binding.navSettingsLabel.setTypeface(null, if (isSettingsSelected) Typeface.BOLD else Typeface.NORMAL)
    }
}
