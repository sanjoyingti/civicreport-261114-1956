package com.arin.civicreport.ui.screens.report

import androidx.lifecycle.ViewModel
import com.arin.civicreport.model.Authority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReportDraft(
    val incidentTypes: List<String> = emptyList(),
    val mediaUri: String? = null,
    val mediaType: String = "photo",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    val description: String = "",
    val authorities: List<Authority> = emptyList(),
    val selectedAuthorityIds: List<String> = emptyList(),
    val sendMethod: String = "both"
) {
    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0
}

class ReportFlowViewModel : ViewModel() {

    private val _draft = MutableStateFlow(ReportDraft())
    val draft: StateFlow<ReportDraft> = _draft.asStateFlow()

    fun start(types: List<String>) {
        _draft.value = ReportDraft(incidentTypes = types)
    }

    fun setMedia(uri: String, mediaType: String) {
        _draft.value = _draft.value.copy(mediaUri = uri, mediaType = mediaType)
    }

    fun setLocation(
        latitude: Double,
        longitude: Double,
        address: String,
        city: String = "",
        district: String = "",
        state: String = "",
        pincode: String = ""
    ) {
        _draft.value = _draft.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = address,
            city = city,
            district = district,
            state = state,
            pincode = pincode
        )
    }

    fun setDescription(text: String) {
        _draft.value = _draft.value.copy(description = text)
    }

    fun setAuthorities(authorities: List<Authority>, selectedIds: List<String>) {
        _draft.value = _draft.value.copy(
            authorities = authorities,
            selectedAuthorityIds = selectedIds
        )
    }

    fun toggleAuthority(id: String) {
        val current = _draft.value.selectedAuthorityIds
        _draft.value = _draft.value.copy(
            selectedAuthorityIds = if (current.contains(id)) current - id else current + id
        )
    }

    fun setSendMethod(method: String) {
        _draft.value = _draft.value.copy(sendMethod = method)
    }

    fun reset() {
        _draft.value = ReportDraft()
    }
}
