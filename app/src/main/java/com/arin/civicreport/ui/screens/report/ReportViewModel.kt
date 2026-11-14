package com.arin.civicreport.ui.screens.report

import androidx.lifecycle.ViewModel
import com.arin.civicreport.model.Authority as AuthorityModel

class ReportViewModel : ViewModel() {
    private var _reportId = 0L
    var reportId: Long
        get() = _reportId
        set(value) { _reportId = value }

    private var _selectedAuthorities = listOf<AuthorityModel>()
    var selectedAuthorities: List<AuthorityModel>
        get() = _selectedAuthorities
        set(value) { _selectedAuthorities = value }

    fun updateAuthorities(authorities: List<AuthorityModel>) {
        _selectedAuthorities = authorities
    }

    fun reportSelectedAuthorities(): List<AuthorityModel> = _selectedAuthorities
}
