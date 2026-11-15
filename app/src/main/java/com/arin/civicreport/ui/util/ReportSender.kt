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

/** Builds the complaint text and hands it to the phone's email / SMS apps. */
object ReportSender {

    fun buildSubject(report: Report): String =
        "Civic complaint: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    fun buildBody(report: Report): String {
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val mapLink = String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
            report.latitude,
            report.longitude
        )
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.append("A civic issue is reported here for your kind action:")
        builder.appendLine()
        builder.appendLine()
        builder.appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Recorded on: $whenText")
        builder.appendLine(
            String.format(Locale.US, "Exact GPS position: %.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine("Map link: $mapLink")
        builder.appendLine("Address: " + report.address.ifBlank { "address not resolved by the device" })
        if (report.city.isNotBlank()) builder.appendLine("City/Town: ${report.city}")
        if (report.district.isNotBlank()) builder.appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) builder.appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: ${report.pincode}")
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Description: ${report.description}")
        }
        builder.appendLine()
        builder.appendLine(
            "Media evidence (" + report.mediaType + "): " +
                if (report.mediaUris.isEmpty()) "not attached" else "${report.mediaUris.size} file(s) attached"
        )
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the needed action.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.append("Sent through DuarBagori Reports")
        return builder.toString()
    }

    private fun attachmentUris(report: Report): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        report.mediaUris.forEach { raw ->
            if (raw.isBlank()) return@forEach
            val uri = Uri.parse(raw)
            val scheme = uri.scheme.orEmpty()
            if (scheme == "content" || scheme == "file") uris.add(uri)
        }
        return uris
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email }.filter { it.isNotBlank() }.distinct()
        val phones = authorities.map { it.phone }.filter { it.isNotBlank() }.distinct()
        val wantEmail = method == "email" || method == "both"
        val wantSms = method == "sms" || method == "both"
        var launched = 0

        if (wantEmail && emails.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (report.mediaUris.isEmpty()) "message/rfc822" else "*/*"
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, buildSubject(report))
                putExtra(Intent.EXTRA_TEXT, buildBody(report))
                val attachments = attachmentUris(report)
                if (attachments.isNotEmpty()) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val single = Intent(Intent.ACTION_SEND).apply {
                type = intent.type
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, buildSubject(report))
                putExtra(Intent.EXTRA_TEXT, buildBody(report))
                val attachments = attachmentUris(report)
                if (attachments.isNotEmpty()) putExtra(Intent.EXTRA_STREAM, attachments.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Send complaint by email").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(single))
            }
            try {
                context.startActivity(chooser)
                launched++
            } catch (e: Exception) {
                Toast.makeText(context, "No email app is installed.", Toast.LENGTH_LONG).show()
            }
        }

        if (wantSms && phones.isNotEmpty()) {
            val body = "Complaint (" + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) } +
                String.format(Locale.US, ") at %.5f,%.5f. ", report.latitude, report.longitude) +
                report.address + ". " + report.description
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:" + phones.joinToString(","))
                putExtra("sms_body", body)
            }
            try {
                context.startActivity(intent)
                launched++
            } catch (e: Exception) {
                Toast.makeText(context, "No SMS app is installed.", Toast.LENGTH_LONG).show()
            }
        }

        if (launched == 0) {
            Toast.makeText(
                context,
                "Saved. No contact method was available on this phone.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
