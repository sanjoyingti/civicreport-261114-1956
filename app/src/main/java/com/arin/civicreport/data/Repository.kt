package com.arin.civicreport.data

import com.arin.civicreport.model.Authority as AuthorityModel
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.model.Report as ReportModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class Repository(private val database: CivicDatabase) {

    suspend fun saveReport(report: ReportModel): Long = withContext(Dispatchers.IO) {
        val row = Report(
            id = report.id,
            incident_types = report.incidentTypes.joinToString(","),
            media_uris = report.mediaUris.filter { it.isNotBlank() }.joinToString(","),
            media_type = report.mediaType,
            timestamp = report.timestamp,
            latitude = report.latitude,
            longitude = report.longitude,
            address = report.address,
            city = report.city,
            district = report.district,
            state = report.state,
            pincode = report.pincode,
            description = report.description,
            status = report.status.name,
            sent_authorities = report.sentAuthorities.joinToString(","),
            last_updated = report.lastUpdated,
            send_method = report.sendMethod
        )
        database.reportDao().insert(row)
    }

    fun getReports(): Flow<List<ReportModel>> =
        database.reportDao().getAll().map { list -> list.map { it.toModel() } }

    suspend fun getReport(id: Long): ReportModel? = withContext(Dispatchers.IO) {
        database.reportDao().getById(id)?.toModel()
    }

    suspend fun updateStatus(id: Long, status: IncidentStatus) = withContext(Dispatchers.IO) {
        database.reportDao().updateStatus(id, status.name, System.currentTimeMillis())
    }

    suspend fun deleteReport(id: Long) = withContext(Dispatchers.IO) {
        database.reportDao().delete(id)
    }

    suspend fun addAuthority(a: AuthorityModel) = withContext(Dispatchers.IO) {
        database.authorityDao().insert(
            Authority(
                id = a.id,
                name = a.name,
                phone = a.phone,
                email = a.email,
                level = a.level,
                activity_tags = a.activityTags.joinToString(","),
                applicable_states = a.applicableStates.joinToString(","),
                applicable_cities = a.applicableCities.joinToString(","),
                `order` = a.order
            )
        )
    }

    suspend fun getAuthority(id: String): AuthorityModel? = withContext(Dispatchers.IO) {
        database.authorityDao().getById(id)?.toModel()
    }

    suspend fun getAuthoritiesForIncident(
        typeTag: String,
        state: String,
        city: String
    ): List<AuthorityModel> = withContext(Dispatchers.IO) {
        database.authorityDao().findByTag(typeTag, state, city).map { it.toModel() }
    }

    suspend fun seedAuthorities() = withContext(Dispatchers.IO) {
        database.authorityDao().insertAll(SeededAuthorities.all())
    }

    private fun Report.toModel() = ReportModel(
        id = id,
        incidentTypes = incident_types.split(",").clean(),
        mediaUris = media_uris.split(",").clean(),
        mediaType = media_type,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        address = address,
        city = city,
        district = district,
        state = state,
        pincode = pincode,
        description = description,
        status = runCatching { IncidentStatus.valueOf(status) }.getOrDefault(IncidentStatus.Draft),
        sentAuthorities = sent_authorities.split(",").clean(),
        lastUpdated = last_updated,
        sendMethod = send_method
    )

    private fun Authority.toModel() = AuthorityModel(
        id = id,
        name = name,
        phone = phone,
        email = email,
        level = level,
        activityTags = activity_tags.split(",").clean(),
        applicableStates = applicable_states.split(",").clean(),
        applicableCities = applicable_cities.split(",").clean(),
        order = `order`
    )

    private fun List<String>.clean(): List<String> = filter { it.isNotBlank() }
}
