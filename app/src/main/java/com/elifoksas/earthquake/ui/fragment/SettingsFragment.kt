package com.elifoksas.earthquake.ui.fragment

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.databinding.FragmentSettingsBinding
import com.elifoksas.earthquake.ui.DistanceFormatter
import com.elifoksas.earthquake.ui.preference.MagnitudePreference
import com.elifoksas.earthquake.ui.preference.SettingsPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SettingsPreferences.migrateRangeDefaults(preferences)
        bindSavedValues()
        setupMagnitudeFilter()
        setupNotificationPreferences()
        setupDistanceRangePreference()
        setupDepthRangePreference()
        setupQuietHoursPreference()
        setupMapTypePreference()
        setupDistanceUnitPreference()
        setupFeedback()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            updateDistanceRangeText()
        }
    }

    private fun bindSavedValues() {
        val magnitude = preferences.getInt(
            MagnitudePreference.KEY,
            MagnitudePreference.DEFAULT_STORED_VALUE
        ).coerceIn(MIN_MAGNITUDE, MAX_MAGNITUDE)

        binding.magnitudeSeekBar.progress = magnitude - MIN_MAGNITUDE
        updateMagnitudeValue(magnitude)
        binding.earthquakeNotificationsSwitch.isChecked =
            preferences.getBoolean(SettingsPreferences.NOTIFICATIONS_KEY, false)
        updateDistanceRangeText()
        updateDepthRangeText()
        updateQuietHoursText()
        updateMapTypeValue(
            preferences.getString(
                SettingsPreferences.MAP_TYPE_KEY,
                SettingsPreferences.DEFAULT_MAP_TYPE
            )
        )
        updateDistanceUnitText()
    }

    private fun setupMagnitudeFilter() {
        binding.magnitudeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val storedValue = MIN_MAGNITUDE + progress
                    updateMagnitudeValue(storedValue)
                    if (fromUser) {
                        preferences.edit()
                            .putInt(MagnitudePreference.KEY, storedValue)
                            .apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            }
        )
    }

    private fun setupNotificationPreferences() {
        binding.earthquakeNotificationsSwitch.setOnCheckedChangeListener { _, checked ->
            preferences.edit().putBoolean(SettingsPreferences.NOTIFICATIONS_KEY, checked).apply()
        }
    }

    private fun setupDistanceRangePreference() {
        binding.distanceRangeSeekBar.max = DISTANCE_RANGES_KM.lastIndex
        binding.distanceRangeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val selectedValue = DISTANCE_RANGES_KM[progress.coerceIn(0, DISTANCE_RANGES_KM.lastIndex)]
                    updateDistanceRangeText(selectedValue)
                    if (fromUser) {
                        preferences.edit()
                            .putInt(SettingsPreferences.DISTANCE_RANGE_KEY, selectedValue)
                            .apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            }
        )
    }

    private fun setupDepthRangePreference() {
        binding.depthRangeSeekBar.max = DEPTH_RANGES_KM.lastIndex
        binding.depthRangeSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val selectedValue = DEPTH_RANGES_KM[progress.coerceIn(0, DEPTH_RANGES_KM.lastIndex)]
                    updateDepthRangeText(selectedValue)
                    if (fromUser) {
                        preferences.edit()
                            .putInt(SettingsPreferences.DEPTH_RANGE_KEY, selectedValue)
                            .apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            }
        )
    }

    private fun setupQuietHoursPreference() {
        binding.quietHoursCard.setOnClickListener {
            val isEnabled = preferences.getBoolean(
                SettingsPreferences.QUIET_HOURS_ENABLED_KEY,
                false
            )
            val labels = arrayOf(
                getString(R.string.settings_quiet_hours_off),
                getString(R.string.settings_quiet_hours_on),
                getString(R.string.settings_quiet_hours_change)
            )

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_quiet_hours_dialog_title)
                .setSingleChoiceItems(labels, if (isEnabled) 1 else 0) { dialog, selectedIndex ->
                    when (selectedIndex) {
                        0 -> {
                            preferences.edit()
                                .putBoolean(SettingsPreferences.QUIET_HOURS_ENABLED_KEY, false)
                                .apply()
                            updateQuietHoursText()
                            dialog.dismiss()
                        }
                        1 -> {
                            preferences.edit()
                                .putBoolean(SettingsPreferences.QUIET_HOURS_ENABLED_KEY, true)
                                .apply()
                            updateQuietHoursText()
                            dialog.dismiss()
                        }
                        else -> {
                            dialog.dismiss()
                            showQuietHoursStartPicker()
                        }
                    }
                }
                .show()
        }
    }

    private fun setupMapTypePreference() {
        binding.mapTypeCard.setOnClickListener {
            val values = resources.getStringArray(R.array.MapsTypeEntryValues)
            val entries = resources.getStringArray(R.array.MapsTypeEntries)
            val currentValue = preferences.getString(
                SettingsPreferences.MAP_TYPE_KEY,
                SettingsPreferences.DEFAULT_MAP_TYPE
            )
            val checkedItem = values.indexOf(currentValue).coerceAtLeast(0)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_map_type_dialog_title)
                .setSingleChoiceItems(entries, checkedItem) { dialog, selectedIndex ->
                    preferences.edit()
                        .putString(SettingsPreferences.MAP_TYPE_KEY, values[selectedIndex])
                        .apply()
                    updateMapTypeValue(values[selectedIndex])
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupDistanceUnitPreference() {
        binding.distanceUnitsCard.setOnClickListener {
            val values = arrayOf(
                SettingsPreferences.DISTANCE_UNIT_KILOMETERS,
                SettingsPreferences.DISTANCE_UNIT_MILES
            )
            val labels = arrayOf(
                getString(R.string.settings_distance_units_kilometers),
                getString(R.string.settings_distance_units_miles)
            )
            val checkedItem = values.indexOf(selectedDistanceUnit()).coerceAtLeast(0)

            showSingleChoiceDialog(
                title = getString(R.string.settings_distance_units_dialog_title),
                labels = labels,
                checkedItem = checkedItem
            ) { selectedIndex ->
                preferences.edit()
                    .putString(SettingsPreferences.DISTANCE_UNIT_KEY, values[selectedIndex])
                    .apply()
                updateDistanceUnitText()
                updateDistanceRangeText()
            }
        }
    }

    private fun setupFeedback() {
        binding.feedbackCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_feedback_subject))
            }

            try {
                startActivity(
                    Intent.createChooser(
                        intent,
                        getString(R.string.settings_feedback_chooser)
                    )
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    R.string.settings_feedback_unavailable,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateMagnitudeValue(storedValue: Int) {
        val value = String.format(
            Locale.getDefault(),
            "%.1f",
            MagnitudePreference.toMagnitude(storedValue)
        )
        binding.magnitudeValue.text = value
    }

    private fun updateDistanceRangeText(valueOverride: Int? = null) {
        val value = valueOverride ?: preferences.getInt(
            SettingsPreferences.DISTANCE_RANGE_KEY,
            SettingsPreferences.DEFAULT_DISTANCE_RANGE_KM
        )
        val displayValue = if (value == SettingsPreferences.ALL_RANGES) {
            getString(R.string.settings_all)
        } else {
            DistanceFormatter.format(value.toDouble(), selectedDistanceUnit())
        }

        binding.distanceRangeSeekBar.progress = distanceRangeProgressFor(value)
        binding.distanceRangeMinLabel.text = DistanceFormatter.format(
            DISTANCE_RANGES_KM.first().toDouble(),
            selectedDistanceUnit()
        )
        binding.distanceRangeMaxLabel.text = getString(R.string.settings_all)
        binding.distanceRangeValue.text = displayValue
        binding.distanceRangeSummary.text = if (hasLocationPermission()) {
            if (value == SettingsPreferences.ALL_RANGES) {
                getString(R.string.settings_all_earthquakes)
            } else {
                getString(R.string.settings_distance_range_summary, displayValue)
            }
        } else {
            getString(R.string.settings_distance_range_location_required)
        }
    }

    private fun distanceRangeProgressFor(value: Int): Int {
        return DISTANCE_RANGES_KM.indexOf(value).takeIf { it >= 0 }
            ?: DISTANCE_RANGES_KM.indexOf(SettingsPreferences.DEFAULT_DISTANCE_RANGE_KM)
                .coerceAtLeast(0)
    }

    private fun updateDepthRangeText(valueOverride: Int? = null) {
        val value = valueOverride ?: preferences.getInt(
            SettingsPreferences.DEPTH_RANGE_KEY,
            SettingsPreferences.DEFAULT_DEPTH_RANGE_KM
        )
        val displayValue = if (value == SettingsPreferences.ALL_RANGES) {
            getString(R.string.settings_all)
        } else {
            getString(R.string.settings_depth_kilometers, value)
        }

        binding.depthRangeSeekBar.progress = depthRangeProgressFor(value)
        binding.depthRangeMinLabel.text = getString(
            R.string.settings_depth_kilometers,
            DEPTH_RANGES_KM.first()
        )
        binding.depthRangeMaxLabel.text = getString(R.string.settings_all)
        binding.depthRangeValue.text = displayValue
        binding.depthRangeSummary.text = if (value == SettingsPreferences.ALL_RANGES) {
            getString(R.string.settings_all_earthquakes)
        } else {
            getString(R.string.settings_depth_range_summary, displayValue)
        }
    }

    private fun depthRangeProgressFor(value: Int): Int {
        return DEPTH_RANGES_KM.indexOf(value).takeIf { it >= 0 }
            ?: DEPTH_RANGES_KM.indexOf(SettingsPreferences.DEFAULT_DEPTH_RANGE_KM)
                .coerceAtLeast(0)
    }

    private fun updateQuietHoursText() {
        val isEnabled = preferences.getBoolean(
            SettingsPreferences.QUIET_HOURS_ENABLED_KEY,
            false
        )
        val start = preferences.getInt(
            SettingsPreferences.QUIET_HOURS_START_MINUTES_KEY,
            SettingsPreferences.DEFAULT_QUIET_HOURS_START_MINUTES
        )
        val end = preferences.getInt(
            SettingsPreferences.QUIET_HOURS_END_MINUTES_KEY,
            SettingsPreferences.DEFAULT_QUIET_HOURS_END_MINUTES
        )

        binding.quietHoursValue.text = getString(
            if (isEnabled) R.string.settings_quiet_hours_on else R.string.settings_quiet_hours_off
        )
        binding.quietHoursSummary.text = "${formatTime(start)} - ${formatTime(end)}"
    }

    private fun updateMapTypeValue(value: String?) {
        val values = resources.getStringArray(R.array.MapsTypeEntryValues)
        val entries = resources.getStringArray(R.array.MapsTypeEntries)
        val index = values.indexOf(value).coerceAtLeast(0)
        binding.mapTypeValue.text = entries[index]
    }

    private fun updateDistanceUnitText() {
        val usesMiles = selectedDistanceUnit() == SettingsPreferences.DISTANCE_UNIT_MILES
        binding.distanceUnitsSummary.text = getString(
            if (usesMiles) {
                R.string.settings_distance_units_miles
            } else {
                R.string.settings_distance_units_kilometers
            }
        )
        binding.distanceUnitsValue.text = if (usesMiles) "mi" else "km"
    }

    private fun showSingleChoiceDialog(
        title: String,
        labels: Array<String>,
        checkedItem: Int,
        onSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(labels, checkedItem) { dialog, selectedIndex ->
                onSelected(selectedIndex)
                dialog.dismiss()
            }
            .show()
    }

    private fun showQuietHoursStartPicker() {
        val currentMinutes = preferences.getInt(
            SettingsPreferences.QUIET_HOURS_START_MINUTES_KEY,
            SettingsPreferences.DEFAULT_QUIET_HOURS_START_MINUTES
        )

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                preferences.edit()
                    .putInt(SettingsPreferences.QUIET_HOURS_START_MINUTES_KEY, hour * 60 + minute)
                    .apply()
                showQuietHoursEndPicker()
            },
            currentMinutes / 60,
            currentMinutes % 60,
            DateFormat.is24HourFormat(requireContext())
        ).apply {
            setTitle(R.string.settings_quiet_hours_start_title)
            show()
        }
    }

    private fun showQuietHoursEndPicker() {
        val currentMinutes = preferences.getInt(
            SettingsPreferences.QUIET_HOURS_END_MINUTES_KEY,
            SettingsPreferences.DEFAULT_QUIET_HOURS_END_MINUTES
        )

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                preferences.edit()
                    .putInt(SettingsPreferences.QUIET_HOURS_END_MINUTES_KEY, hour * 60 + minute)
                    .putBoolean(SettingsPreferences.QUIET_HOURS_ENABLED_KEY, true)
                    .apply()
                updateQuietHoursText()
            },
            currentMinutes / 60,
            currentMinutes % 60,
            DateFormat.is24HourFormat(requireContext())
        ).apply {
            setTitle(R.string.settings_quiet_hours_end_title)
            show()
        }
    }

    private fun selectedDistanceUnit(): String {
        return preferences.getString(
            SettingsPreferences.DISTANCE_UNIT_KEY,
            SettingsPreferences.DEFAULT_DISTANCE_UNIT
        ) ?: SettingsPreferences.DEFAULT_DISTANCE_UNIT
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatTime(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return if (DateFormat.is24HourFormat(requireContext())) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val suffix = if (hour < 12) "AM" else "PM"
            val displayHour = when (val normalized = hour % 12) {
                0 -> 12
                else -> normalized
            }
            String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MIN_MAGNITUDE = 10
        private const val MAX_MAGNITUDE = 100
        private val DISTANCE_RANGES_KM = listOf(25, 50, 100, 250, 500, 1000, 1500, -1)
        private val DEPTH_RANGES_KM = listOf(10, 30, 50, 100, 300, -1)
    }
}
