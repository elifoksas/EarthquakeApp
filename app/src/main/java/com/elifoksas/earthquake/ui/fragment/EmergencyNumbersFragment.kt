package com.elifoksas.earthquake.ui.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.elifoksas.earthquake.R
import com.elifoksas.earthquake.data.entity.CountryEmergencyProfile
import com.elifoksas.earthquake.data.entity.EmergencyService
import com.elifoksas.earthquake.data.entity.PersonalEmergencyContact
import com.elifoksas.earthquake.data.repository.EmergencyNumbersRepository
import com.elifoksas.earthquake.databinding.DialogEmergencyContactBinding
import com.elifoksas.earthquake.databinding.EmergencyContactItemBinding
import com.elifoksas.earthquake.databinding.EmergencyServiceItemBinding
import com.elifoksas.earthquake.databinding.FragmentEmergencyNumbersBinding
import com.elifoksas.earthquake.ui.viewmodel.EmergencyNumbersUiState
import com.elifoksas.earthquake.ui.viewmodel.EmergencyNumbersViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyNumbersFragment : Fragment() {

    private var _binding: FragmentEmergencyNumbersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EmergencyNumbersViewModel by viewModels()
    private var latestState: EmergencyNumbersUiState? = null

    private val contactPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let(::savePickedContact)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyNumbersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.countrySelector.setOnClickListener { showCountryPicker() }
        binding.addContactButton.setOnClickListener { showAddContactOptions() }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestState = state
            render(state)
        }
    }

    private fun render(state: EmergencyNumbersUiState) {
        val profile = state.selectedProfile
        binding.countryNameText.text =
            profile?.countryName ?: getString(R.string.emergency_numbers_country_unknown)
        binding.countryModeText.setText(
            if (state.isAutomatic) {
                R.string.emergency_numbers_automatic
            } else {
                R.string.emergency_numbers_manual
            }
        )
        renderServices(profile)
        renderContacts(state.contacts)

        binding.sourceText.isVisible = profile != null
        if (profile != null) {
            val sourceName = if (profile.sourceUrl.contains("itu.int")) {
                getString(R.string.emergency_numbers_source_itu)
            } else {
                getString(R.string.emergency_numbers_source_other)
            }
            binding.sourceText.text = getString(
                R.string.emergency_numbers_source,
                sourceName,
                profile.verifiedAt
            )
        }
    }

    private fun renderServices(profile: CountryEmergencyProfile?) {
        binding.servicesContainer.removeAllViews()
        val services = profile?.services.orEmpty()
        binding.unavailableText.isVisible = services.isEmpty()

        services.forEach { service ->
            val itemBinding = EmergencyServiceItemBinding.inflate(
                layoutInflater,
                binding.servicesContainer,
                false
            )
            itemBinding.categoryText.text = formatCategories(service)
            itemBinding.numberText.text = service.number
            itemBinding.callButton.setOnClickListener { openDialer(service.number) }
            itemBinding.root.setOnClickListener { openDialer(service.number) }
            binding.servicesContainer.addView(itemBinding.root)
        }
    }

    private fun formatCategories(service: EmergencyService): String {
        return service.categories.map { category ->
            when (category) {
                "GENERAL" -> getString(R.string.emergency_numbers_category_general)
                "POLICE" -> getString(R.string.emergency_numbers_category_police)
                "FIRE" -> getString(R.string.emergency_numbers_category_fire)
                "AMBULANCE" -> getString(R.string.emergency_numbers_category_ambulance)
                else -> category.lowercase().replaceFirstChar(Char::titlecase)
            }
        }.joinToString(" · ")
    }

    private fun renderContacts(contacts: List<PersonalEmergencyContact>) {
        binding.contactCountText.text = getString(
            R.string.emergency_contacts_count,
            contacts.size,
            EmergencyNumbersRepository.MAX_CONTACTS
        )
        binding.emptyContactsText.isVisible = contacts.isEmpty()
        binding.contactsContainer.removeAllViews()

        contacts.forEach { contact ->
            val itemBinding = EmergencyContactItemBinding.inflate(
                layoutInflater,
                binding.contactsContainer,
                false
            )
            itemBinding.contactName.text = contact.name
            itemBinding.contactNumber.text = contact.phoneNumber
            itemBinding.callButton.setOnClickListener { openDialer(contact.phoneNumber) }
            itemBinding.editButton.setOnClickListener { showContactDialog(contact) }
            itemBinding.deleteButton.setOnClickListener { confirmDelete(contact) }
            binding.contactsContainer.addView(itemBinding.root)
        }
    }

    private fun showCountryPicker() {
        val state = latestState ?: return
        val automaticName = state.automaticCountryIso
            ?.let { isoCode -> state.profiles.firstOrNull { it.isoCode == isoCode } }
            ?.countryName
            ?: getString(R.string.emergency_numbers_country_unknown)
        val labels = buildList {
            add(getString(R.string.emergency_numbers_automatic_option, automaticName))
            addAll(state.profiles.map { it.countryName })
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.emergency_numbers_country_label)
            .setItems(labels) { _, which ->
                if (which == 0) {
                    viewModel.selectCountry(null)
                } else {
                    viewModel.selectCountry(state.profiles[which - 1].isoCode)
                }
            }
            .setNegativeButton(R.string.emergency_contacts_cancel, null)
            .show()
    }

    private fun showAddContactOptions() {
        val contacts = latestState?.contacts.orEmpty()
        if (contacts.size >= EmergencyNumbersRepository.MAX_CONTACTS) {
            showToast(R.string.emergency_contacts_limit)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.emergency_contacts_add_title)
            .setItems(
                arrayOf(
                    getString(R.string.emergency_contacts_add_from_phone),
                    getString(R.string.emergency_contacts_add_manually)
                )
            ) { _, which ->
                if (which == 0) {
                    openContactPicker()
                } else {
                    showContactDialog(null)
                }
            }
            .show()
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        try {
            contactPicker.launch(intent)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.emergency_contacts_picker_unavailable)
        }
    }

    private fun savePickedContact(uri: Uri) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val contact = requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val numberIndex = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            val number = if (numberIndex >= 0) cursor.getString(numberIndex) else null
            if (name.isNullOrBlank() || number.isNullOrBlank()) {
                null
            } else {
                name to number
            }
        }

        if (contact == null) {
            showToast(R.string.emergency_contacts_read_failed)
            return
        }
        handleContactResult(viewModel.saveContact(null, contact.first, contact.second))
    }

    private fun showContactDialog(contact: PersonalEmergencyContact?) {
        val dialogBinding = DialogEmergencyContactBinding.inflate(layoutInflater)
        dialogBinding.contactNameInput.setText(contact?.name.orEmpty())
        dialogBinding.contactPhoneInput.setText(contact?.phoneNumber.orEmpty())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (contact == null) {
                    R.string.emergency_contacts_add_title
                } else {
                    R.string.emergency_contacts_edit_title
                }
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.emergency_contacts_cancel, null)
            .setPositiveButton(R.string.emergency_contacts_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = viewModel.saveContact(
                    contact?.id,
                    dialogBinding.contactNameInput.text?.toString().orEmpty(),
                    dialogBinding.contactPhoneInput.text?.toString().orEmpty()
                )
                if (result == EmergencyNumbersRepository.ContactSaveResult.SAVED) {
                    dialog.dismiss()
                } else {
                    handleContactResult(result)
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(contact: PersonalEmergencyContact) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.emergency_contacts_delete_title)
            .setMessage(getString(R.string.emergency_contacts_delete_message, contact.name))
            .setNegativeButton(R.string.emergency_contacts_cancel, null)
            .setPositiveButton(R.string.emergency_contacts_delete) { _, _ ->
                viewModel.deleteContact(contact.id)
            }
            .show()
    }

    private fun handleContactResult(result: EmergencyNumbersRepository.ContactSaveResult) {
        val message = when (result) {
            EmergencyNumbersRepository.ContactSaveResult.SAVED -> return
            EmergencyNumbersRepository.ContactSaveResult.INVALID ->
                R.string.emergency_contacts_invalid
            EmergencyNumbersRepository.ContactSaveResult.DUPLICATE ->
                R.string.emergency_contacts_duplicate
            EmergencyNumbersRepository.ContactSaveResult.LIMIT_REACHED ->
                R.string.emergency_contacts_limit
        }
        showToast(message)
    }

    private fun openDialer(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.emergency_numbers_no_dialer)
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
