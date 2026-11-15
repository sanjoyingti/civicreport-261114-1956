package com.arin.civicreport.ui.util

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

/** Turns a report into an email / SMS draft and hands it to the phone's own apps. */
object ReportSender {

    fun send(context: Context, targets: List<Authority>, report: Report, method: String) {
        if (method == "email" || method == "both") sendEmail(context, targets, report)
        if (method == "sms" || method == "both") sendSms(context, targets, report)
    }

    fun resend(context: Context, targets: List<Authority>, report: Report) {
        send(context, targets, report, report.sendMethod.ifBlank { "both" })
    }

    private fun subject(report: Report): String =
        "Civic complaint: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    private fun body(report: Report): String {
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        return buildString {
            appendLine("Dear Sir / Madam,")
            appendLine()
            appendLine("I wish to report the following civic / environmental problem:")
            appendLine("Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
            if (report.description.isNotBlank()) appendLine("Details: " + report.description)
            appendLine("Address: " + report.address.ifBlank { "not available" })
            val place = listOf(report.city, report.district, report.state, report.pincode)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            if (place.isNotBlank()) appendLine("Area: $place")
            appendLine(
                String.format(
                    Locale.US,
                    "Exact GPS location: %.6f, %.6f",
                    report.latitude,
                    report.longitude
                )
            )
            appendLine("Map link: https://www.google.com/maps?q=${report.latitude},${report.longitude}")
            appendLine("Reported on: $whenText")
            appendLine("Media evidence type: " + report.mediaType)
            if (report.mediaUris.isNotEmpty()) {
                appendLine("Evidence files attached: " + report.mediaUris.size)
            }
            appendLine()
            appendLine("Kindly inspect the spot and take necessary action.")
            appendLine()
            appendLine("Regards,")
            appendLine("A concerned citizen (sent through DuarBagori Reports)")
        }
    }

    private fun sendEmail(context: Context, targets: List<Authority>, report: Report) {
        val emails = targets.map { it.email }.filter { it.isNotBlank() }.toTypedArray()
        if (emails.isEmpty()) {
            Toast.makeText(context, "No email address saved for these offices", Toast.LENGTH_LONG)
                .show()
            return
        }
        val attachments = report.mediaUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        val intent = if (attachments.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                attachments.firstOrNull()?.let { putExtra(Intent.EXTRA_STREAM, it) }
            }
        }
        intent.type = if (report.mediaType == "video") "video/*" else "image/*"
        if (attachments.isEmpty()) intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_EMAIL, emails)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject(report))
        intent.putExtra(Intent.EXTRA_TEXT, body(report))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launch(context, Intent.createChooser(intent, "Send complaint by email"))
    }

    private fun sendSms(context: Context, targets: List<Authority>, report: Report) {
        val numbers = targets.map { it.phone }.filter { it.isNotBlank() }
            .map { it.replace(" ", "") }
        if (numbers.isEmpty()) {
            Toast.makeText(context, "No phone number saved for these offices", Toast.LENGTH_LONG)
                .show()
            return
        }
        val uri = Uri.parse("smsto:" + numbers.joinToString(","))
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        intent.putExtra("sms_body", body(report))
        launch(context, Intent.createChooser(intent, "Send complaint by message"))
    }

    private fun launch(context: Context, intent: Intent) {
        try {
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app could handle this: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }
}
