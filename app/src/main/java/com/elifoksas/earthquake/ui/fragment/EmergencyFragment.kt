package com.elifoksas.earthquake.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.EmergencyItem
import com.elifoksas.earthquake.databinding.FragmentEmergencyBinding
import com.elifoksas.earthquake.ui.adapter.EmergencyAdapter
import com.elifoksas.earthquake.ui.viewmodel.EmergencyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyFragment : Fragment() {

    private lateinit var viewModel: EmergencyViewModel
    private lateinit var binding: FragmentEmergencyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tempViewModel : EmergencyViewModel by viewModels()
        viewModel = tempViewModel
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmergencyBinding.inflate(inflater)

        val layoutManager = LinearLayoutManager(context)
        binding.rv.layoutManager = layoutManager

        viewModel.emergencyItems.observe(viewLifecycleOwner){
            val adapter = EmergencyAdapter(requireContext(),it, object:
            EmergencyAdapter.OnItemClickListener{
                override fun onItemClick(item: EmergencyItem) {
                    handleItemClick(item)
                }

            })
            binding.rv.adapter = adapter
        }


        return binding.root
    }

    private fun handleItemClick(item: EmergencyItem){
        when(item.emergencyName){
            "Whistle" ->
                navigateToWhistleFragment()

            "What to Do During an Earthquake?" ->
                navigateToInformationFragment()
            }

    }

    private fun navigateToWhistleFragment() {
        findNavController().navigate(
            R.id.action_emergencyFragment_to_whistleFragment,
            null,
            navOptions {
                popUpTo(R.id.emergencyFragment) {
                    inclusive = false
                }
            }
        )
    }

    private fun navigateToInformationFragment() {
        findNavController().navigate(
            R.id.action_emergencyFragment_to_informationFragment,
            null,
            navOptions {
                popUpTo(R.id.informationFragment) {
                    inclusive = false
                }
            }
        )
    }

}