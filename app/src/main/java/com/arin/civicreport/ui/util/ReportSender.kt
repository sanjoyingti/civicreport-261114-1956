=== ABOUT: New helper that writes the complaint text and hands it to the phone's email or message app, addressed to the chosen officials ===
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

/** Builds the complaint text and opens the phone's email / SMS app with all recipients filled in. */
object ReportSender {

    fun buildBody(report: Report, authorities: List<Authority>): String {
        val stamp = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        return buildString {
            appendLine("CIVIC INCIDENT REPORT")
            appendLine()
            appendLine("To:")
            authorities.forEach { appendLine(" - ${it.name} (${it.level})") }
            appendLine()
            appendLine("Incident type(s): ${report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }}")
            appendLine("Reported on: $stamp")
            appendLine("Address: ${report.address.ifBlank { "not available" }}")
            if (report.district.isNotBlank() || report.state.isNotBlank()) {
                appendLine("District: ${report.district}  State: ${report.state}  PIN: ${report.pincode}")
            }
            appendLine("GPS coordinates: ${"%.6f".format(report.latitude)}, ${"%.6f".format(report.longitude)}")
            appendLine("Map link: https://www.openstreetmap.org/?mlat=${"%.6f".format(report.latitude)}&mlon=${"%.6f".format(report.longitude)}#map=18/${"%.6f".format(report.latitude)}/${"%.6f".format(report.longitude)}")
            appendLine()
            appendLine("Details: ${report.description.ifBlank { "no extra description provided" }}")
            appendLine()
            append("Kindly inspect the site and take necessary action. The photo/video proof captured at the exact spot is attached. ")
            append("Report reference: #${report.id}")
        }
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email }.filter { it.isNotBlank() }.distinct()
        val phones = authorities.map { it.phone }.filter { it.isNotBlank() }.distinct()
        val body = buildBody(report, authorities)
        val media = report.mediaUris.firstOrNull()

        if (method != "sms" && emails.isNotEmpty()) {
            sendEmail(context, emails, report, body, media)
        }
        if (method != "email" && phones.isNotEmpty()) {
            sendSms(context, phones, body)
        }
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        report: Report,
        body: String,
        media: String?
    ) {
        val subject = "Civic complaint: ${report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }} at ${report.address.ifBlank { "reported location" }}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (media != null) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launch(context, intent, "No email app found. Install one to send by email.")
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + phones.joinToString(","))
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launch(context, intent, "No SMS app found. Install one to send by SMS.")
    }

    private fun launch(context: Context, intent: Intent, errorMessage: String) {
        try {
            context.startActivity(Intent.createChooser(intent, "Send report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
