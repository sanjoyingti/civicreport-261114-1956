package com.arin.civicreport.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arin.civicreport.model.Authority as AuthorityModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val _reportId = MutableStateFlow(0L)
    val reportId: StateFlow<Long> = _reportId.asStateFlow()

    fun setReportId(id: Long) {
        _reportId.value = id
    }

    private val _selectedAuthorities = MutableStateFlow<List<AuthorityModel>>(emptyList())
    val selectedAuthorities: StateFlow<List<AuthorityModel>> = _selectedAuthorities.asStateFlow()

    fun updateAuthorities(authorities: List<AuthorityModel>) {
        _selectedAuthorities.value = authorities
    }

    fun reportSelectedAuthorities(): List<AuthorityModel> = _selectedAuthorities.value
}
