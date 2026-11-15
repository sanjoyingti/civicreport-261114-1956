package com.arin.civicreport.ui.screens.report

import androidx.lifecycle.ViewModel
import com.arin.civicreport.model.Authority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Everything collected while the user builds one report. */
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
        _draft.update { it.copy(incidentTypes = types) }
    }

    fun setMedia(uri: String, type: String) {
        _draft.update { it.copy(mediaUri = uri, mediaType = type) }
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

    fun setDescription(text: String) {
        _draft.update { it.copy(description = text) }
    }

    fun setAuthorities(list: List<Authority>) {
        _draft.update {
            it.copy(authorities = list, selectedAuthorityIds = list.map { a -> a.id }.toSet())
        }
    }

    fun toggleAuthority(id: String) {
        _draft.update { current ->
            val next = if (current.selectedAuthorityIds.contains(id))
                current.selectedAuthorityIds - id
            else current.selectedAuthorityIds + id
            current.copy(selectedAuthorityIds = next)
        }
    }

    fun selectAll(selected: Boolean) {
        _draft.update {
            it.copy(
                selectedAuthorityIds = if (selected) it.authorities.map { a -> a.id }.toSet()
                else emptySet()
            )
        }
    }
}
