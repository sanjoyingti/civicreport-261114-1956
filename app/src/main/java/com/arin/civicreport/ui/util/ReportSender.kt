package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a saved report into a ready-to-send email or SMS addressed to the chosen offices.
 * The user only has to press send in their own mail/SMS app, so no server is needed.
 */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { clean(it.email) }.filter { it.contains("@") }.distinct()
        val phones = authorities.mapNotNull { sanitisePhone(it.phone) }.distinct()

        var opened = 0
        if (method == "email" || method == "both") {
            if (emails.isNotEmpty() && openEmail(context, emails, report)) opened++
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty() && openSms(context, phones, report)) opened++
        }

        if (opened == 0) {
            Toast.makeText(
                context,
                "No email or SMS app could be opened. The report is saved on this phone.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Complaint prepared for ${authorities.size} office(s). Press send in the app that opened.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun clean(value: String): String? = value.trim().takeIf { it.isNotEmpty() }

    private fun sanitisePhone(value: String): String? =
        value.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
            .takeIf { it.isNotEmpty() }

    private fun openEmail(context: Context, emails: List<String>, report: Report): Boolean {
        return try {
            val attachment = resolveShareableUri(context, report.mediaUris.firstOrNull())
            val mime = when {
                attachment == null -> "message/rfc822"
                report.mediaType == "video" -> "video/mp4"
                else -> "image/jpeg"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, buildSubject(report))
                putExtra(Intent.EXTRA_TEXT, buildBody(report))
                if (attachment != null) {
                    putExtra(Intent.EXTRA_STREAM, attachment)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = android.content.ClipData.newRawUri("evidence", attachment)
                }
            }
            val chooser = Intent.createChooser(intent, "Send complaint to ${emails.size} office(s)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun openSms(context: Context, phones: List<String>, report: Report): Boolean {
        return try {
            val uri = Uri.parse("sms:" + phones.joinToString(","))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                putExtra("sms_body", buildSms(report))
                putExtra("exit_on_sent", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveShareableUri(context: Context, raw: String?): Uri? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return try {
            val uri = Uri.parse(value)
            when (uri.scheme?.lowercase(Locale.US)) {
                "content" -> uri
                "file" -> {
                    val file = uri.path?.let { File(it) } ?: return null
                    if (!file.exists()) return null
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun incidentTitles(report: Report): String =
        report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    private fun buildSubject(report: Report): String {
        val place = report.address.ifBlank { "reported location" }
        return "Civic complaint: ${incidentTitles(report)} at $place"
    }

    private fun buildBody(report: Report): String = buildString {
        appendLine("Sir / Madam,")
        appendLine()
        appendLine("I wish to report the following civic issue for your kind action:")
        appendLine()
        appendLine("Issue: ${incidentTitles(report)}")
        appendLine("Address: ${report.address.ifBlank { "Not available" }}")
        if (report.city.isNotBlank()) appendLine("City: ${report.city}")
        if (report.district.isNotBlank()) appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) appendLine("PIN code: ${report.pincode}")
        appendLine(String.format(Locale.US, "GPS position: %.6f, %.6f", report.latitude, report.longitude))
        appendLine("Map link: https://maps.google.com/?q=${report.latitude},${report.longitude}")
        appendLine(
            "Recorded on: " +
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        )
        if (report.description.isNotBlank()) {
            appendLine()
            appendLine("Notes: ${report.description}")
        }
        appendLine()
        appendLine("Photo / video evidence of the spot is attached.")
        appendLine()
        appendLine("Requesting the necessary action at the earliest.")
        appendLine()
        appendLine("Regards,")
        appendLine("Sent from DuarBagori Reports")
    }

    private fun buildSms(report: Report): String = buildString {
        append("Civic complaint: ")
        append(incidentTitles(report))
        append(" at ")
        append(report.address.ifBlank { "reported spot" })
        append(String.format(Locale.US, " (GPS %.6f, %.6f)", report.latitude, report.longitude))
        if (report.description.isNotBlank()) {
            append(". Notes: ")
            append(report.description)
        }
        append(". Map: https://maps.google.com/?q=${report.latitude},${report.longitude}")
        append(". Sent from DuarBagori Reports.")
    }
}
