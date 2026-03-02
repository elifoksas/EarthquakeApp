package com.elifoksas.earthquake.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.elifoksas.earthquake.data.entity.EmergencyItem
import com.elifoksas.earthquake.databinding.FragmentEmergencyBinding
import com.elifoksas.earthquake.databinding.FragmentInformationBinding
import com.elifoksas.earthquake.ui.adapter.EmergencyAdapter
import com.elifoksas.earthquake.ui.adapter.InformationAdapter
import com.elifoksas.earthquake.ui.viewmodel.InformationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InformationFragment : Fragment() {

    private lateinit var binding: FragmentInformationBinding
    private lateinit var viewModel: InformationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tempviewModel : InformationViewModel by viewModels()
        viewModel = tempviewModel

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInformationBinding.inflate(inflater)


        viewModel.informationItems.observe(viewLifecycleOwner){
            val adapter = InformationAdapter(requireContext(), it)
            binding.rv.layoutManager = LinearLayoutManager(requireContext())
            binding.rv.adapter = adapter
        }

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { handleBackButtonClick() }


    }


    private fun handleBackButtonClick(){

        //navigate back to emergency fragment
        findNavController().popBackStack() // Navigate back

    }
}