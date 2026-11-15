package com.arin.civicreport.ui.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the complaint text and hands it to the phone's email app or SMS app. */
object ReportSender {

    private const val SMS_PIECE = 150

    fun buildSubject(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val place = report.city.ifBlank { report.district.ifBlank { report.state } }
        return "Civic complaint: $types" + if (place.isNotBlank()) " at $place" else ""
    }

    fun buildBody(report: Report, authorities: List<Authority>): String = buildString {
        appendLine("Civic complaint filed with DuarBagori Reports")
        appendLine()
        appendLine("Incident: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        appendLine(
            "Filed on: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(report.timestamp))
        )
        appendLine("Address: " + report.address.ifBlank { "not available" })
        appendLine(
            "GPS: " + String.format(
                Locale.US, "%.6f, %.6f", report.latitude, report.longitude
            )
        )
        if (report.latitude != 0.0 || report.longitude != 0.0) {
            appendLine(
                "Map: " + String.format(
                    Locale.US,
                    "https://www.google.com/maps?q=%.6f,%.6f",
                    report.latitude,
                    report.longitude
                )
            )
        }
        val admin = listOf(report.city, report.district, report.state, report.pincode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (admin.isNotBlank()) appendLine("City / District / State: $admin")
        if (report.description.isNotBlank()) {
            appendLine()
            appendLine("Notes from the reporter:")
            appendLine(report.description)
        }
        appendLine()
        appendLine("Media attached: ${report.mediaUris.size} file(s) of type ${report.mediaType}")
        appendLine()
        appendLine("Addressed to:")
        authorities.forEach { authority ->
            appendLine("- ${authority.name} <${authority.email.ifBlank { authority.phone }}>")
        }
        appendLine()
        appendLine("Sent from the DuarBagori Reports app.")
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email.trim() }
            .filter { it.contains("@") }
            .distinct()
        val phones = authorities.map { it.phone.replace(Regex("[^0-9+]"), "") }
            .filter { it.isNotBlank() }
            .distinct()

        val subject = buildSubject(report)
        val body = buildBody(report, authorities)

        if (method != "sms" && emails.isNotEmpty()) {
            composeEmail(context, emails, subject, body, report.mediaUris)
        }
        if (method != "email" && phones.isNotEmpty()) {
            sendSms(context, phones, body)
        }
    }

    fun call(context: Context, phone: String) {
        val number = phone.replace(Regex("[^0-9+]"), "")
        if (number.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        } catch (e: ActivityNotFoundException) {
            // No dialer installed: nothing else to do.
        }
    }

    fun emailOne(context: Context, address: String, subject: String, body: String) {
        if (!address.contains("@")) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            composeEmail(context, listOf(address), subject, body, emptyList())
        }
    }

    private fun composeEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        media: List<String>
    ) {
        val uris = ArrayList<Uri>()
        media.forEach { raw ->
            if (raw.isNotBlank()) {
                runCatching { Uri.parse(raw) }.getOrNull()?.let { uris.add(it) }
            }
        }
        val intent = if (uris.isNotEmpty()) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeOf(uris.first())
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        }
        intent.putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            val chooser = Intent.createChooser(intent, "Send complaint by email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            openMailto(context, emails, subject, body)
        }
    }

    private fun openMailto(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String
    ) {
        try {
            val intent = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:" + emails.joinToString(","))
            ).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Nothing can handle email on this device.
        }
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val manager = smsManager(context)
            if (manager != null) {
                try {
                    val pieces = if (body.length > SMS_PIECE) body.chunked(SMS_PIECE) else listOf(body)
                    phones.forEach { phone ->
                        pieces.forEach { piece ->
                            manager.sendTextMessage(phone, null, piece, null, null)
                        }
                    }
                    return
                } catch (e: Exception) {
                    // Fall through to the composer.
                }
            }
        }
        openSmsComposer(context, phones, body)
    }

    private fun smsManager(context: Context): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    } catch (e: Exception) {
        null
    }

    private fun openSmsComposer(context: Context, phones: List<String>, body: String) {
        try {
            val intent = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + phones.joinToString(","))
            ).apply {
                putExtra("sms_body", body)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No messaging app installed.
        }
    }

    private fun mimeOf(uri: Uri): String {
        val value = uri.toString().lowercase(Locale.US)
        return if (value.endsWith(".mp4") || value.contains("video")) "video/*" else "image/*"
    }
}
