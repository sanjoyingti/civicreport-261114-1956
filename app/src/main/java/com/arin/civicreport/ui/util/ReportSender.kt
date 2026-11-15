package com.arin.civicreport.ui.util

import android.Manifest
import android.content.ActivityNotFoundException
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

/** Builds the complaint message and pushes it into the phone's email / SMS app. */
object ReportSender {

    private fun stamp(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

    fun subject(report: Report): String =
        "Civic complaint: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    fun body(report: Report): String {
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following civic issue:")
        builder.appendLine()
        builder.appendLine("Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Reported on: " + stamp(report.timestamp))
        builder.appendLine("Address: " + report.address.ifBlank { "not available" })
        if (report.city.isNotBlank()) builder.appendLine("City/Town: ${report.city}")
        if (report.district.isNotBlank()) builder.appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) builder.appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: ${report.pincode}")
        builder.appendLine(
            String.format(Locale.US, "GPS coordinates: %.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine(
            String.format(
                Locale.US,
                "Map link: https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=18/%.6f/%.6f",
                report.latitude, report.longitude, report.latitude, report.longitude
            )
        )
        builder.appendLine()
        if (report.description.isNotBlank()) {
            builder.appendLine("Details: ${report.description}")
            builder.appendLine()
        }
        builder.appendLine(
            "Media attached: " + when {
                report.mediaUris.isEmpty() -> "none"
                report.mediaType == "video" -> "1 video clip (see attachment)"
                else -> "1 photograph (see attachment)"
            }
        )
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the necessary action at the earliest.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.appendLine("A concerned citizen (sent from the DuarBagori Reports app)")
        return builder.toString()
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.trim().takeIf { value -> value.isNotBlank() } }
        val phones = authorities.mapNotNull { it.phone.trim().takeIf { value -> value.isNotBlank() } }
        val text = body(report)

        val wantsSms = method == "sms" || method == "both"
        val wantsEmail = method == "email" || method == "both"

        if (wantsSms && phones.isNotEmpty()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                sendSms(context, phones, text)
            } else {
                openSmsIntent(context, phones, text)
            }
        }
        if (wantsEmail && emails.isNotEmpty()) {
            openEmailIntent(context, emails, subject(report), text, report)
        }
    }

    private fun sendSms(context: Context, phones: List<String>, text: String) {
        val manager = ContextCompat.getSystemService(context, SmsManager::class.java)
        if (manager == null) {
            openSmsIntent(context, phones, text)
            return
        }
        val parts = manager.divideMessage(text)
        var sentCount = 0
        phones.forEach { number ->
            try {
                manager.sendMultipartTextMessage(number, null, parts, null, null)
                sentCount++
            } catch (e: Exception) {
                // fall through, handled below
            }
        }
        if (sentCount > 0) {
            Toast.makeText(
                context,
                "SMS queued for $sentCount office(s)",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            openSmsIntent(context, phones, text)
        }
    }

    private fun openSmsIntent(context: Context, phones: List<String>, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + phones.joinToString(","))
            putExtra("sms_body", text)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmailIntent(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            val media = report.mediaUris.firstOrNull()
            if (media != null && media.isNotBlank()) {
                val uri = Uri.parse(media)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, "report media", uri)
            }
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send complaint email"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}
