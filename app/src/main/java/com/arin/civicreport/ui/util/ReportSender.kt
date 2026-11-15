package com.arin.civicreport.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Composes the complaint and hands it to the email / SMS app installed on the phone. */
object ReportSender {

    private const val TAG = "CivicReport"

    fun buildSubject(report: Report): String {
        val kinds = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val place = report.address.ifBlank { report.city.ifBlank { "the reported location" } }
        return "Complaint: $kinds at $place"
    }

    fun buildBody(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I wish to report the following civic / environmental problem through the DuarBagori Reports app.")
        builder.appendLine()
        builder.appendLine("Incident: $types")
        builder.appendLine("Reported on: $whenText")
        builder.appendLine("Address: ${report.address.ifBlank { "not resolved by the geocoder" }}")
        if (report.district.isNotBlank()) builder.appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) builder.appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: ${report.pincode}")
        builder.appendLine(
            "GPS: ${String.format(Locale.US, "%.6f", report.latitude)}, " +
                String.format(Locale.US, "%.6f", report.longitude)
        )
        builder.appendLine(
            "Map: https://www.google.com/maps/search/?api=1&query=" +
                "${report.latitude},${report.longitude}"
        )
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: ${report.description}")
        }
        if (report.mediaUris.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("A ${report.mediaType} of the spot is attached / sent along and is stored with this complaint.")
        }
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the needed action. Please treat this as a formal grievance.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.appendLine("A citizen using DuarBagori Reports")
        return builder.toString()
    }

    fun buildSms(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val gps = String.format(Locale.US, "%.5f,%.5f", report.latitude, report.longitude)
        val place = report.address.ifBlank { report.city.ifBlank { "unknown" } }
        val notes = if (report.description.isNotBlank()) " Notes: ${report.description}" else ""
        return "Complaint ($types) at $place. GPS: $gps. Map: " +
            "https://maps.google.com/?q=$gps.$notes - sent via DuarBagori Reports"
    }

    /** Opens the email and/or SMS app. Errors are swallowed so the saved report is never lost. */
    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email }.filter { it.isNotBlank() }.distinct()
        val phones = authorities.map { it.phone }.filter { it.isNotBlank() }.distinct()

        if ((method == "email" || method == "both") && emails.isNotEmpty()) {
            launch(context, emailIntent(context, emails, report))
        }
        if ((method == "sms" || method == "both") && phones.isNotEmpty()) {
            launch(context, smsIntent(phones, report))
        }
    }

    /** Resend helper for one report from the details screen. */
    fun resend(context: Context, authorities: List<Authority>, report: Report) {
        val method = when {
            authorities.any { it.email.isNotBlank() } && report.sendMethod == "both" -> "both"
            report.sendMethod.isBlank() -> "both"
            else -> report.sendMethod
        }
        send(context, authorities, report, method)
    }

    private fun emailIntent(context: Context, recipients: List<String>, report: Report): Intent {
        val media = report.mediaUris.firstOrNull()
        val intent = if (media != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = if (report.mediaType == "video") "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
            }
        }
        intent.putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
        intent.putExtra(Intent.EXTRA_SUBJECT, buildSubject(report))
        intent.putExtra(Intent.EXTRA_TEXT, buildBody(report))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun smsIntent(phones: List<String>, report: Report): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + phones.joinToString(","))
            putExtra("sms_body", buildSms(report))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch ${intent.action}", e)
        }
    }
}
