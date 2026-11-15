package com.arin.civicreport.ui.util

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val subject = subjectFor(report)
        val body = bodyFor(report)
        if (method == "email" || method == "both") {
            val emails = authorities.mapNotNull { it.email.trim().takeIf(String::isNotBlank) }
            if (emails.isNotEmpty()) {
                sendEmail(context, emails, subject, body, report.mediaUris)
            }
        }
        if (method == "sms" || method == "both") {
            val phones = authorities.mapNotNull { it.phone.trim().takeIf(String::isNotBlank) }
            if (phones.isNotEmpty()) {
                sendSms(context, phones, body)
            }
        }
    }

    fun resend(context: Context, authorities: List<Authority>, report: Report) {
        send(context, authorities, report, report.sendMethod)
    }

    private fun subjectFor(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Civic complaint: $types at ${report.address.ifBlank { "reported location" }}"
    }

    private fun bodyFor(report: Report): String {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        return buildString {
            appendLine("Civic complaint filed with DuarBagori Reports")
            appendLine()
            appendLine("Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
            appendLine("Filed on: $stamp")
            appendLine("Location: " + report.address.ifBlank { "unknown address" })
            appendLine(
                String.format(
                    Locale.US,
                    "GPS coordinates: %.6f, %.6f",
                    report.latitude,
                    report.longitude
                )
            )
            if (report.city.isNotBlank()) appendLine("City/Town: ${report.city}")
            if (report.district.isNotBlank()) appendLine("District: ${report.district}")
            if (report.state.isNotBlank()) appendLine("State: ${report.state}")
            if (report.pincode.isNotBlank()) appendLine("PIN: ${report.pincode}")
            appendLine("Media attached: ${report.mediaType.ifBlank { "none" }}")
            if (report.description.isNotBlank()) {
                appendLine()
                appendLine("Notes from the citizen:")
                appendLine(report.description)
            }
            appendLine()
            appendLine("Please inspect the spot and take corrective action. Kindly acknowledge this complaint.")
        }
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        mediaUris: List<String>
    ) {
        val uris = mediaUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        val mime = when {
            mediaUris.isEmpty() -> "text/plain"
            else -> "*/*"
        }
        val intent = if (uris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                uris.firstOrNull()?.let { putExtra(Intent.EXTRA_STREAM, it) }
            }
        }
        intent.putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        if (uris.isNotEmpty()) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newRawUri("report media", uris.first())
        }
        launch(context, Intent.createChooser(intent, "Send complaint by email"))
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val manager = ContextCompat.getSystemService(context, SmsManager::class.java)
                ?: SmsManager.getDefault()
            val parts = manager.divideMessage(body)
            var anySent = false
            phones.forEach { phone ->
                try {
                    manager.sendMultipartTextMessage(phone, null, parts, null, null)
                    anySent = true
                } catch (e: Exception) {
                    // try the next number
                }
            }
            if (anySent) return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${phones.first()}")
            putExtra("sms_body", body)
        }
        launch(context, intent)
    }

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No mail or SMS app installed: the report stays saved on the device.
        }
    }
}
