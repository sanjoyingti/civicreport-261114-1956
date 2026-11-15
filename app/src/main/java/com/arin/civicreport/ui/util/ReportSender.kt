package com.arin.civicreport.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the complaint text and hands it to the phone's email / messaging app,
 * addressed to the authorities the user picked. Media is attached for email.
 */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.takeIf { e -> e.isNotBlank() } }
        val phones = authorities.mapNotNull { it.phone.takeIf { p -> p.isNotBlank() } }
        var launched = false

        if (method != "sms" && emails.isNotEmpty()) {
            launched = true
            val chooser = Intent.createChooser(
                emailIntent(context, emails, report),
                "Send complaint by email"
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found.", Toast.LENGTH_LONG).show()
            }
        }

        if (method != "email" && phones.isNotEmpty()) {
            launched = true
            val chooser = Intent.createChooser(
                smsIntent(phones, report),
                "Send complaint by SMS"
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "No messaging app found.", Toast.LENGTH_LONG).show()
            }
        }

        if (!launched) {
            Toast.makeText(
                context,
                "No email address or phone number saved for these offices.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun emailIntent(context: Context, emails: List<String>, report: Report): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject(report))
            putExtra(Intent.EXTRA_TEXT, longBody(report))
        }
        val uriString = report.mediaUris.firstOrNull { it.isNotBlank() }
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            intent.type = if (report.mediaType == "video") "video/*" else "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.clipData = ClipData.newUri(context.contentResolver, "report media", uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun smsIntent(phones: List<String>, report: Report): Intent {
        val uri = Uri.parse("smsto:" + phones.joinToString(","))
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", shortBody(report))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun subject(report: Report): String {
        val titles = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val place = report.address.ifBlank { report.city.ifBlank { "location on map" } }
        return "Civic complaint: $titles at $place"
    }

    private fun longBody(report: Report): String = buildString {
        appendLine("Respected Sir / Madam,")
        appendLine()
        appendLine("I wish to report the following civic/environmental issue.")
        appendLine()
        appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        appendLine("Reported on: " + dateText(report.timestamp))
        appendLine("Address: " + report.address.ifBlank { "not available from the geocoder" })
        if (report.city.isNotBlank()) appendLine("City / Town: ${report.city}")
        if (report.district.isNotBlank()) appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) appendLine("PIN code: ${report.pincode}")
        appendLine("GPS coordinates: ${coords(report)}")
        appendLine("Map link: ${mapLink(report)}")
        if (report.description.isNotBlank()) {
            appendLine()
            appendLine("Details: ${report.description}")
        }
        appendLine()
        appendLine("A photo/video captured at the exact spot is attached.")
        appendLine()
        appendLine("Kindly inspect the site and take the necessary action at the earliest.")
        appendLine()
        appendLine("Thanking you,")
        appendLine("A concerned citizen")
        appendLine("(Sent through the DuarBagori Reports app)")
    }

    private fun shortBody(report: Report): String {
        val titles = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Civic complaint: $titles at ${report.address.ifBlank { coords(report) }}. " +
            "GPS ${coords(report)}. Map ${mapLink(report)}." +
            if (report.description.isNotBlank()) " Notes: ${report.description}" else ""
    }

    private fun coords(report: Report): String =
        String.format(Locale.US, "%.6f, %.6f", report.latitude, report.longitude)

    private fun mapLink(report: Report): String =
        "https://www.openstreetmap.org/?mlat=${report.latitude}&mlon=${report.longitude}#map=17/${report.latitude}/${report.longitude}"

    private fun dateText(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
