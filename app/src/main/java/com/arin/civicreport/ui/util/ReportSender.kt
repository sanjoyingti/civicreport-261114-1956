package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hands the complaint to whatever email or SMS app is installed. The report itself is always
 * already saved in the database before this runs, so nothing is lost if the user backs out.
 */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.trim().takeIf(String::isNotEmpty) }.distinct()
        val phones = authorities.mapNotNull { it.phone.trim().takeIf(String::isNotEmpty) }.distinct()
        val subject = buildSubject(report)
        val body = buildBody(authorities, report)

        if ((method == "email" || method == "both") && emails.isNotEmpty()) {
            launchEmail(context, emails, subject, body, report)
        }
        if ((method == "sms" || method == "both") && phones.isNotEmpty()) {
            launchSms(context, phones, body)
        }
    }

    fun buildSubject(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Complaint: $types near ${report.city.ifBlank { report.district }.ifBlank { report.state }}"
    }

    fun buildBody(authorities: List<Authority>, report: Report): String {
        val when_ = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following civic/environmental issue:")
        builder.appendLine()
        builder.appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Date and time: $when_")
        builder.appendLine("Address: " + report.address.ifBlank { "not resolved" })
        if (report.city.isNotBlank()) builder.appendLine("City / Town: ${report.city}")
        if (report.district.isNotBlank()) builder.appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) builder.appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN code: ${report.pincode}")
        builder.appendLine(
            "GPS coordinates: " + String.format(
                Locale.US,
                "%.6f, %.6f",
                report.latitude,
                report.longitude
            )
        )
        builder.appendLine("Map link: https://www.openstreetmap.org/?mlat=${report.latitude}&mlon=${report.longitude}#map=17/${report.latitude}/${report.longitude}")
        builder.appendLine()
        builder.appendLine("Details: " + report.description.ifBlank { "Please inspect the spot at the coordinates above." })
        builder.appendLine()
        builder.appendLine("Photo/video evidence is attached or available from this device (${report.mediaType}).")
        builder.appendLine()
        builder.appendLine("Addressed to:")
        authorities.forEach { builder.appendLine("- ${it.name} (${it.level})") }
        builder.appendLine()
        builder.appendLine("Kindly look into the matter and take necessary action at the earliest.")
        builder.appendLine()
        builder.appendLine("Thanking you,")
        builder.appendLine("A concerned citizen")
        builder.appendLine()
        builder.appendLine("Filed with DuarBagori Reports")
        return builder.toString()
    }

    private fun launchEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ) {
        val uris = report.mediaUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        val intent = if (uris.isEmpty()) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:" + emails.joinToString(","))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (report.mediaType == "video") "video/*" else "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        start(context, intent, "Send complaint by email")
    }

    private fun launchSms(context: Context, phones: List<String>, body: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("smsto:" + phones.joinToString(","))
            putExtra("sms_body", body)
        }
        start(context, intent, "Send complaint by SMS")
    }

    private fun start(context: Context, intent: Intent, title: String) {
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (ignored: ActivityNotFoundException) {
                // nothing installed to handle it; the saved report still holds everything
            }
        }
    }
}
