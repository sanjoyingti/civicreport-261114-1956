package com.arin.civicreport.ui.util

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds and launches the email / SMS complaint for a report. */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val subject = "Civic complaint: " +
            report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val body = buildBody(report)
        var launched = false

        if (method == "email" || method == "both") {
            if (sendEmail(context, authorities, subject, body, report.mediaUris.firstOrNull())) {
                launched = true
            }
        }
        if (method == "sms" || method == "both") {
            if (sendSms(context, authorities, body)) {
                launched = true
            }
        }
        if (!launched) {
            Toast.makeText(
                context,
                "No email or SMS app is available on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildBody(report: Report): String {
        val when_ = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val mapLink = "https://www.openstreetmap.org/?mlat=${report.latitude}&mlon=${report.longitude}#map=18/${report.latitude}/${report.longitude}"
        return buildString {
            appendLine("Respected Sir/Madam,")
            appendLine()
            appendLine(
                "I would like to report the following civic problem: " +
                    report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) } + "."
            )
            appendLine()
            appendLine("Details of the spot:")
            appendLine("Address: ${report.address.ifBlank { "see coordinates" }}")
            if (report.state.isNotBlank()) appendLine("State: ${report.state}")
            if (report.district.isNotBlank()) appendLine("District: ${report.district}")
            if (report.city.isNotBlank()) appendLine("City / town: ${report.city}")
            if (report.pincode.isNotBlank()) appendLine("PIN code: ${report.pincode}")
            appendLine(
                "GPS: ${String.format(Locale.US, "%.6f", report.latitude)}, " +
                    String.format(Locale.US, "%.6f", report.longitude)
            )
            appendLine("Map: $mapLink")
            appendLine("Reported on: $when_")
            if (report.description.isNotBlank()) {
                appendLine()
                appendLine("Notes: ${report.description}")
            }
            if (report.mediaUris.isNotEmpty()) {
                appendLine()
                appendLine("Photo / video evidence is attached.")
            }
            appendLine()
            appendLine("Kindly inspect the spot and take the needed action.")
            appendLine()
            appendLine("Regards,")
            appendLine("A concerned citizen (sent with DuarBagori Reports)")
        }
    }

    private fun sendEmail(
        context: Context,
        authorities: List<Authority>,
        subject: String,
        body: String,
        mediaUri: String?
    ): Boolean {
        val emails = authorities.map { it.email.trim() }.filter { it.isNotBlank() }.distinct()
        if (emails.isEmpty()) return false

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (mediaUri != null) {
                if (mediaUri.contains(".mp4")) "video/*" else "image/*"
            } else {
                "text/plain"
            }
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (mediaUri != null) {
                val uri = Uri.parse(mediaUri)
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "evidence", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Send complaint by email"))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendSms(context: Context, authorities: List<Authority>, body: String): Boolean {
        val numbers = authorities.map { it.phone.trim() }.filter { it.isNotBlank() }.distinct()
        if (numbers.isEmpty()) return false

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            var sent = 0
            val manager = try {
                SmsManager.getDefault()
            } catch (e: Exception) {
                null
            }
            if (manager != null) {
                numbers.forEach { number ->
                    try {
                        val parts = manager.divideMessage(body)
                        manager.sendMultipartTextMessage(number, null, parts, null, null)
                        sent++
                    } catch (e: Exception) {
                        // try the next one
                    }
                }
            }
            if (sent > 0) {
                Toast.makeText(
                    context,
                    "Text message queued for $sent office(s).",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }

        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:" + numbers.joinToString(";"))
        ).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
