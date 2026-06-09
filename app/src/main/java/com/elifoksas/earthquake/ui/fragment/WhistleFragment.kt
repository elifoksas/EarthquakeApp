package com.elifoksas.earthquake.ui.fragment

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.databinding.FragmentWhistleBinding
import com.elifoksas.earthquake.ui.viewmodel.WhistleViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhistleFragment : Fragment() {

    private var _binding: FragmentWhistleBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WhistleViewModel
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tempViewModel: WhistleViewModel by viewModels()
        viewModel = tempViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhistleBinding.inflate(inflater, container, false)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.whistle_sound).apply {
            isLooping = true
        }

        binding.backButton.setOnClickListener { handleBackButtonClick() }
        binding.whistleButton.setOnClickListener { handleWhistleButtonClick() }
        renderPlaybackState()

        return binding.root
    }

    private fun handleWhistleButtonClick() {
        if (isPlaying) {
            stopWhistle()
        } else {
            mediaPlayer?.start()
            isPlaying = true
        }
        renderPlaybackState()
    }

    private fun handleBackButtonClick() {
        stopWhistle()
        findNavController().popBackStack()
    }

    private fun stopWhistle() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                player.seekTo(0)
            }
        }
        isPlaying = false
    }

    private fun renderPlaybackState() {
        val titleColor = if (isPlaying) {
            R.color.earthquake_accent
        } else {
            R.color.earthquake_text_primary
        }

        binding.statusTitle.setText(
            if (isPlaying) R.string.whistle_active else R.string.whistle_ready
        )
        binding.statusTitle.setTextColor(ContextCompat.getColor(requireContext(), titleColor))
        binding.statusDescription.setText(
            if (isPlaying) {
                R.string.whistle_active_description
            } else {
                R.string.whistle_ready_description
            }
        )
        binding.actionText.setText(
            if (isPlaying) R.string.whistle_stop else R.string.whistle_start
        )
        binding.whistleButton.setBackgroundResource(
            if (isPlaying) {
                R.drawable.whistle_action_active_background
            } else {
                R.drawable.whistle_action_ready_background
            }
        )
        binding.whistleButton.contentDescription = getString(
            if (isPlaying) R.string.whistle_stop else R.string.whistle_start
        )
    }

    override fun onPause() {
        stopWhistle()
        if (_binding != null) {
            renderPlaybackState()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        stopWhistle()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
        super.onDestroyView()
    }
}
