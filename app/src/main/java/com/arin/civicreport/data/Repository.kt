package com.arin.civicreport.data

import com.arin.civicreport.model.Authority as AuthorityModel
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.model.Report as ReportModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

class Repository(private val database: CivicDatabase) {

    suspend fun saveReport(report: ReportModel): Long = withContext(Dispatchers.IO) {
        val r = Report(
            id = report.id,
            incident_types = report.incidentTypes.joinToString(","),
            media_uris = report.mediaUris.joinToString(","),
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
        database.reportDao().insert(r)
    }

    fun getReports(): Flow<List<ReportModel>> = database.reportDao().getAll().flowOn(Dispatchers.IO).map { list ->
        list.map { r ->
            ReportModel(
                id = r.id,
                incidentTypes = r.incident_types.split(",").filterOrNull(),
                mediaUris = r.media_uris.split(",").filterOrNull(),
                mediaType = r.media_type,
                timestamp = r.timestamp,
                latitude = r.latitude,
                longitude = r.longitude,
                address = r.address,
                city = r.city,
                district = r.district,
                state = r.state,
                pincode = r.pincode,
                description = r.description,
                status = IncidentStatus.valueOf(r.status),
                sentAuthorities = r.sent_authorities.split(",").filterOrNull(),
                lastUpdated = r.last_updated,
                sendMethod = r.send_method
            )
        }
    }

    suspend fun updateStatus(id: Long, status: IncidentStatus) =
        withContext(Dispatchers.IO) {
            database.reportDao().updateStatus(id, status.name, System.currentTimeMillis())
        }

    suspend fun deleteReport(id: Long) = withContext(Dispatchers.IO) {
        database.reportDao().delete(id)
    }

    suspend fun addAuthority(a: AuthorityModel) = withContext(Dispatchers.IO) {
        val x = Authority(
            id = a.id, name = a.name, phone = a.phone, email = a.email,
            level = a.level, activity_tags = a.activityTags.joinToString(","),
            applicable_states = a.applicableStates.joinToString(","),
            applicable_cities = a.applicableCities.joinToString(","),
            `order` = a.order
        )
        database.authorityDao().insert(x)
    }

    suspend fun getAuthoritiesForIncident(typeTag: String, state: String, city: String): List<AuthorityModel> =
        withContext(Dispatchers.IO) {
            database.authorityDao().findByTag(typeTag, state, city)
                .map { a ->
                    AuthorityModel(
                        id = a.id, name = a.name, phone = a.phone, email = a.email,
                        level = a.level, activityTags = a.activity_tags.split(",").filterOrNull(),
                        applicableStates = a.applicable_states.split(",").filterOrNull(),
                        applicableCities = a.applicable_cities.split(",").filterOrNull(),
                        order = a.`order`
                    )
                }
        }

    suspend fun seedAuthorities() = withContext(Dispatchers.IO) {
        database.authorityDao().insertAll(SeededAuthorities.all())
    }

    private fun List<String>.filterOrNull(): List<String> = filter { it.isNotBlank() }
}
