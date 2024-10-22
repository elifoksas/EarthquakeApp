package com.techgeeksclub.earthquake.ui.fragment

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.techgeeksclub.earthquake.R
import com.techgeeksclub.earthquake.databinding.FragmentWhistleBinding
import com.techgeeksclub.earthquake.ui.viewmodel.WhistleViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhistleFragment : Fragment() {

    private lateinit var binding: FragmentWhistleBinding
    private lateinit var viewModel: WhistleViewModel
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlaying = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tempViewModel: WhistleViewModel by viewModels()
        viewModel = tempViewModel

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentWhistleBinding.inflate(inflater)

        //whistle sound
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.whistle_sound).apply {
            isLooping = true //looping the sound
        }

        binding.backButton.setOnClickListener { handleBackButtonClick() }
        binding.whistleButton.setOnClickListener { handleWhistleButtonClick() }

        return binding.root
    }
    private fun handleWhistleButtonClick(){
        //playing whistle sound when button is clicked
        if (isPlaying) {
            binding.whistleButton.setBackgroundResource(R.drawable.whistle_off)
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        } else {
            mediaPlayer.start()
            binding.whistleButton.setBackgroundResource(R.drawable.whistle_on)
        }
        isPlaying = !isPlaying
    }

    private fun handleBackButtonClick(){
        //pause whistle if the switch is still checked
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }

        isPlaying = false

        //navigate back to emergency fragment
        findNavController().popBackStack() // Navigate back

    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()

        // Ensure WhistleFragment is removed from the back stack
        findNavController().popBackStack(R.id.whistleFragment, true)
    }

    override fun onPause() {
        super.onPause()

        // Ensure WhistleFragment is removed from the back stack
        findNavController().popBackStack(R.id.whistleFragment, true)
    }


}