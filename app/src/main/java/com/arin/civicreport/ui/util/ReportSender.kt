package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
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

/** Builds the complaint and opens the phone's email or SMS app for the chosen offices. */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.takeIf { mail -> mail.isNotBlank() } }
        val phones = authorities.mapNotNull { it.phone.takeIf { number -> number.isNotBlank() } }
        if (emails.isEmpty() && phones.isEmpty()) {
            toast(context, "The selected offices have no email or phone number")
            return
        }
        val subject = buildSubject(report)
        val body = buildBody(report, authorities)
        var opened = false
        if (method == "email" || method == "both") {
            if (emails.isNotEmpty()) opened = openEmail(context, emails, subject, body, report) || opened
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty()) opened = openSms(context, phones, body) || opened
        }
        if (!opened) toast(context, "No email or SMS app is installed on this phone")
    }

    private fun buildSubject(report: Report): String {
        val titles = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val place = report.city.ifBlank { report.district }.ifBlank { report.state }
        return "Complaint: $titles${if (place.isNotBlank()) " at $place" else ""}"
    }

    private fun buildBody(report: Report, authorities: List<Authority>): String {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val mapsLink = "https://maps.google.com/?q=${report.latitude},${report.longitude}"
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine(
            "I would like to report the following civic/environmental issue that needs urgent attention."
        )
        builder.appendLine()
        builder.appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Date and time of the incident: $stamp")
        builder.appendLine("Address: " + report.address.ifBlank { "Not available" })
        builder.appendLine(
            "Exact GPS: " + String.format(Locale.US, "%.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine("Map: $mapsLink")
        builder.appendLine("Media evidence: " + if (report.mediaType == "video") "video attached" else "photo attached")
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Additional notes: " + report.description)
        }
        builder.appendLine()
        builder.appendLine("Offices addressed: " + authorities.joinToString("; ") { it.name })
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the necessary action. Please acknowledge this complaint.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.appendLine("A concerned citizen")
        return builder.toString()
    }

    private fun openEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ): Boolean {
        val media = report.mediaUris.firstOrNull()
        if (!media.isNullOrBlank()) {
            val type = if (report.mediaType == "video") "video/*" else "image/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return start(context, Intent.createChooser(intent, "Send complaint with evidence"))
        }
        val mailto = "mailto:" + emails.joinToString(",") +
            "?subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body)
        return start(context, Intent(Intent.ACTION_SENDTO, Uri.parse(mailto)))
    }

    private fun openSms(context: Context, phones: List<String>, body: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + phones.joinToString(",") { it.replace(" ", "") })
            putExtra("sms_body", body)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            if (intent !is Intent || intent.resolveActivity(context.packageManager) == null) {
                false
            } else {
                context.startActivity(intent)
                true
            }
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
