package com.arin.civicreport.ui.util

import android.Manifest
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

/** Builds the complaint text and hands it to the phone's email or SMS app. */
object ReportSender {

    fun composeMessage(report: Report): String {
        val builder = StringBuilder()
        builder.appendLine("CIVIC COMPLAINT - DuarBagori Reports")
        builder.appendLine()
        builder.appendLine(
            "Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        )
        builder.appendLine("Location: " + report.address.ifBlank { "unknown" })
        builder.appendLine(
            String.format(Locale.US, "GPS: %.6f, %.6f", report.latitude, report.longitude)
        )
        if (report.city.isNotBlank()) builder.appendLine("City: ${report.city}")
        if (report.district.isNotBlank()) builder.appendLine("District: ${report.district}")
        if (report.state.isNotBlank()) builder.appendLine("State: ${report.state}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: ${report.pincode}")
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: ${report.description}")
        }
        builder.appendLine()
        builder.appendLine(
            "One ${if (report.mediaType == "video") "video" else "photo"} is attached with this complaint."
        )
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        builder.append("Filed on $whenText.")
        return builder.toString()
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.trim().takeIf(String::isNotBlank) }
        val phones = authorities
            .mapNotNull { it.phone.trim().takeIf(String::isNotBlank) }
            .map { it.replace(Regex("[^+0-9]"), "") }
            .filter { it.isNotBlank() }
        val message = composeMessage(report)
        if (method == "email" || method == "both") {
            if (emails.isNotEmpty()) sendEmail(context, emails, report, message)
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty()) sendSms(context, phones, message)
        }
    }

    private fun subject(report: Report): String {
        val type = report.incidentTypes.firstOrNull()?.let { IncidentType.titleFor(it) } ?: "Civic issue"
        val place = report.address.take(60).ifBlank { "reported location" }
        return "Complaint: $type at $place"
    }

    private fun sendEmail(context: Context, emails: List<String>, report: Report, message: String) {
        val mime = when (report.mediaType) {
            "video" -> "video/mp4"
            "photo" -> "image/jpeg"
            else -> "text/plain"
        }
        val body = message + "\n\nSent to: " + emails.joinToString(", ")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject(report))
            putExtra(Intent.EXTRA_TEXT, body)
            val media = report.mediaUris.firstOrNull { it.isNotBlank() }
            if (media != null) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Send complaint by email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            try {
                val mailTo = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + emails.joinToString(","))
                ).apply {
                    putExtra(Intent.EXTRA_SUBJECT, subject(report))
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mailTo)
            } catch (inner: Exception) {
                Toast.makeText(context, "No email app could be opened", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendSms(context: Context, phones: List<String>, message: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            try {
                val manager = context.getSystemService(SmsManager::class.java)
                if (manager != null) {
                    phones.forEach { phone ->
                        val parts = manager.divideMessage(message)
                        if (parts.size > 1) {
                            manager.sendMultipartTextMessage(phone, null, parts, null, null)
                        } else {
                            manager.sendTextMessage(phone, null, message, null, null)
                        }
                    }
                    Toast.makeText(
                        context,
                        "SMS queued to ${phones.size} office(s)",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            } catch (e: Exception) {
                // fall through to the messaging app
            }
        }
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:" + phones.joinToString(","))
        ).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No messaging app could be opened", Toast.LENGTH_LONG).show()
        }
    }
}
