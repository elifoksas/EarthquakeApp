package com.elifoksas.earthquake.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.elifoksas.earthquake.R
import java.util.Locale
import kotlin.math.roundToInt

class DecimalSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SeekBarPreference(context, attrs) {

    private val scale = MagnitudePreference.SCALE.toInt()
    private val valueMarginStart = 16.dp
    private var valueTextView: TextView? = null
    private var isTrackingTouch = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DecimalSeekBarPreference)
        val minDecimalValue = typedArray.getFloat(
            R.styleable.DecimalSeekBarPreference_minDecimalValue,
            0f
        )
        val maxDecimalValue = typedArray.getFloat(
            R.styleable.DecimalSeekBarPreference_maxDecimalValue,
            10f
        )
        val decimalStep = typedArray.getFloat(
            R.styleable.DecimalSeekBarPreference_decimalStep,
            0.1f
        )
        typedArray.recycle()

        min = toStoredValue(minDecimalValue)
        max = toStoredValue(maxDecimalValue)
        seekBarIncrement = toStoredValue(decimalStep).coerceAtLeast(1)
        showSeekBarValue = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        valueTextView = holder.findViewById(androidx.preference.R.id.seekbar_value) as? TextView
        valueTextView?.visibility = View.VISIBLE
        valueTextView?.addStartMargin(valueMarginStart)
        updateValueText(value)

        val seekBar = holder.findViewById(androidx.preference.R.id.seekbar) as? SeekBar
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val storedValue = min + progress
                updateValueText(storedValue)

                if (fromUser && (updatesContinuously || !isTrackingTouch)) {
                    syncValue(seekBar, storedValue)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isTrackingTouch = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isTrackingTouch = false
                val storedValue = min + seekBar.progress
                if (storedValue != value) {
                    syncValue(seekBar, storedValue)
                }
            }
        })
    }

    private fun syncValue(seekBar: SeekBar, storedValue: Int) {
        if (storedValue == value) {
            return
        }

        if (callChangeListener(storedValue)) {
            setValue(storedValue)
        } else {
            seekBar.progress = value - min
            updateValueText(value)
        }
    }

    override fun setValue(seekBarValue: Int) {
        super.setValue(seekBarValue)
        updateValueText(value)
    }

    private fun toStoredValue(value: Float): Int = (value * scale).roundToInt()

    private fun updateValueText(storedValue: Int) {
        valueTextView?.text = String.format(
            Locale.getDefault(),
            "%.1f",
            MagnitudePreference.toMagnitude(storedValue)
        )
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).roundToInt()

    private fun View.addStartMargin(margin: Int) {
        val marginLayoutParams = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        marginLayoutParams.marginStart = margin
        layoutParams = marginLayoutParams
    }
}
