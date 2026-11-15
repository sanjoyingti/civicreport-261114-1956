package com.arin.civicreport.ui.util

import android.Manifest
import android.app.Activity
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

/** Opens the e-mail app / sends SMS so a report reaches the selected offices. */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email }.filter { it.isNotBlank() }
        val phones = authorities.map { it.phone }.filter { it.isNotBlank() }
        val subject = subjectFor(report)
        val body = bodyFor(report)

        if (method == "email" || method == "both") {
            if (emails.isNotEmpty()) {
                openEmail(context, emails, subject, body, report.mediaUris.firstOrNull(), report.mediaType)
            }
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty()) {
                sendSms(context, phones, shortMessage(subject, report))
            }
        }
    }

    fun resend(context: Context, authorities: List<Authority>, report: Report) {
        send(context, authorities, report, report.sendMethod.ifBlank { "both" })
    }

    private fun subjectFor(report: Report): String {
        val issue = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Civic complaint: $issue at ${report.address.ifBlank { "reported location" }}"
    }

    private fun bodyFor(report: Report): String {
        val builder = StringBuilder()
        builder.appendLine("Sir / Madam,")
        builder.appendLine()
        builder.appendLine("I wish to report the following civic problem:")
        builder.appendLine("Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Location: " + report.address.ifBlank { "not provided" })
        builder.appendLine(String.format(Locale.US, "GPS coordinates: %.6f, %.6f", report.latitude, report.longitude))
        val area = listOf(report.city, report.district, report.state)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (area.isNotBlank()) builder.appendLine("City / District / State: $area")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN code: ${report.pincode}")
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: ${report.description}")
        }
        builder.appendLine()
        builder.appendLine(
            "Reported on: " +
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        )
        builder.appendLine("A photo/video of the spot is attached.")
        builder.appendLine()
        builder.append("Regards,")
        builder.appendLine()
        builder.append("A concerned citizen (sent with DuarBagori Reports)")
        return builder.toString()
    }

    private fun shortMessage(subject: String, report: Report): String {
        val text = StringBuilder()
        text.append(subject)
        text.append(" | GPS: ")
        text.append(String.format(Locale.US, "%.6f, %.6f", report.latitude, report.longitude))
        if (report.description.isNotBlank()) {
            text.append(" | ")
            text.append(report.description)
        }
        text.append(" | via DuarBagori Reports")
        return if (text.length > 320) text.substring(0, 317) + "..." else text.toString()
    }

    private fun start(context: Context, intent: Intent) {
        val safeIntent = if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            intent
        }
        try {
            context.startActivity(safeIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app on this phone can open that", Toast.LENGTH_LONG).show()
        }
    }

    private fun openEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        mediaUri: String?,
        mediaType: String
    ) {
        if (mediaUri.isNullOrBlank()) {
            val mailto = Uri.parse("mailto:" + emails.joinToString(","))
            val intent = Intent(Intent.ACTION_SENDTO, mailto).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            start(context, intent)
            return
        }
        val uri = Uri.parse(mediaUri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (mediaType == "video") "video/*" else "image/*"
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("report media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(context, Intent.createChooser(intent, "Send complaint with e-mail"))
    }

    private fun sendSms(context: Context, phones: List<String>, message: String) {
        val canSend = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canSend) {
            val uri = Uri.parse("smsto:" + phones.joinToString(","))
            start(context, Intent(Intent.ACTION_SENDTO, uri).putExtra("sms_body", message))
            return
        }
        var sent = 0
        phones.forEach { raw ->
            val number = raw.filter { it.isDigit() || it == '+' }
            if (number.isBlank()) return@forEach
            try {
                val manager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                manager?.sendTextMessage(number, null, message, null, null)
                sent++
            } catch (e: Exception) {
                // Keep going so one bad number does not stop the rest.
            }
        }
        if (sent > 0) {
            Toast.makeText(context, "SMS queued for $sent official(s)", Toast.LENGTH_SHORT).show()
        } else {
            val uri = Uri.parse("smsto:" + phones.joinToString(","))
            start(context, Intent(Intent.ACTION_SENDTO, uri).putExtra("sms_body", message))
        }
    }
}
