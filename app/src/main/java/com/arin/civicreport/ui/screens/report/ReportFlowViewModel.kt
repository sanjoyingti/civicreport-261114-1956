package com.arin.civicreport.ui.screens.report

import androidx.lifecycle.ViewModel
import com.arin.civicreport.model.Authority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Everything the user has entered so far for the report in progress. */
data class ReportDraft(
    val incidentTypes: List<String> = emptyList(),
    val mediaUri: String? = null,
    val mediaType: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    val description: String = "",
    val authorities: List<Authority> = emptyList(),
    val selectedAuthorityIds: Set<String> = emptySet()
) {
    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0
}

class ReportFlowViewModel : ViewModel() {

    private val _draft = MutableStateFlow(ReportDraft())
    val draft: StateFlow<ReportDraft> = _draft.asStateFlow()

    fun reset() {
        _draft.value = ReportDraft()
    }

    fun start(types: List<String>) {
        _draft.value = ReportDraft(incidentTypes = types)
    }

    fun setMedia(uri: String, mediaType: String) {
        _draft.update { it.copy(mediaUri = uri, mediaType = mediaType) }
    }

    fun setLocation(
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        district: String,
        state: String,
        pincode: String
    ) {
        _draft.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                address = address,
                city = city,
                district = district,
                state = state,
                pincode = pincode
            )
        }
    }

    fun setDescription(description: String) {
        _draft.update { it.copy(description = description) }
    }

    fun setAuthorities(authorities: List<Authority>, preselected: Set<String>) {
        _draft.update { it.copy(authorities = authorities, selectedAuthorityIds = preselected) }
    }

    fun toggleAuthority(id: String) {
        _draft.update {
            val next = if (it.selectedAuthorityIds.contains(id)) {
                it.selectedAuthorityIds - id
            } else {
                it.selectedAuthorityIds + id
            }
            it.copy(selectedAuthorityIds = next)
        }
    }

    fun selectAll() {
        _draft.update { it.copy(selectedAuthorityIds = it.authorities.map { a -> a.id }.toSet()) }
    }

    fun clearSelection() {
        _draft.update { it.copy(selectedAuthorityIds = emptySet()) }
    }
}
