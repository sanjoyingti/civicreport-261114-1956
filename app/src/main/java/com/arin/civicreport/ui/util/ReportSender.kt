package com.arin.civicreport.ui.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the complaint text and hands it to the phone's email app and/or messaging app.
 * Nothing is sent silently: the user always sees the populated message and presses send.
 */
object ReportSender {

    fun send(
        context: Context,
        authorities: List<Authority>,
        report: Report,
        method: String
    ) {
        if (authorities.isEmpty()) {
            Toast.makeText(context, "No officials selected", Toast.LENGTH_LONG).show()
            return
        }

        val message = buildMessage(report, authorities)
        val subject = buildSubject(report)
        val emails = authorities.mapNotNull { cleanEmail(it.email) }.distinct()
        val phones = authorities.mapNotNull { cleanPhone(it.phone) }.distinct()

        val wantsEmail = (method == "email" || method == "both") && emails.isNotEmpty()
        val wantsSms = (method == "sms" || method == "both") && phones.isNotEmpty()

        if (!wantsEmail && !wantsSms) {
            Toast.makeText(
                context,
                "The selected offices have no usable email or phone number",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (wantsEmail) {
            sendEmail(context, emails, subject, message, report)
        }
        if (wantsSms) {
            if (wantsEmail) {
                Handler(Looper.getMainLooper()).postDelayed({
                    sendSms(context, phones, message)
                }, 800L)
            } else {
                sendSms(context, phones, message)
            }
        }
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        message: String,
        report: Report
    ) {
        val uris = report.mediaUris
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }

        val intent = try {
            if (uris.isEmpty()) {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:" + emails.joinToString(","))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, message)
                }
            } else {
                val mime = when {
                    report.mediaType == "video" -> "video/mp4"
                    uris.any { it.toString().lowercase(Locale.US).contains("mp4") } -> "video/mp4"
                    else -> "image/jpeg"
                }
                if (uris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, message)
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = mime
                        putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, message)
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }

        if (intent == null) {
            Toast.makeText(context, "The email app could not be prepared", Toast.LENGTH_LONG).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found on this phone", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "The email app could not be opened", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSms(context: Context, phones: List<String>, message: String) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            openSmsComposer(context, phones, message)
            return
        }

        try {
            val manager = ContextCompat.getSystemService(context, SmsManager::class.java)
                ?: SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            phones.forEach { phone ->
                manager.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            Toast.makeText(
                context,
                "SMS sent to ${phones.size} office(s)",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            openSmsComposer(context, phones, message)
        }
    }

    private fun openSmsComposer(context: Context, phones: List<String>, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:" + phones.joinToString(","))
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No messaging app found on this phone", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildSubject(report: Report): String {
        val what = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val place = report.city.ifBlank { report.district }.ifBlank { report.state }
        return "Civic complaint: $what" + if (place.isNotBlank()) " at $place" else ""
    }

    private fun buildMessage(report: Report, authorities: List<Authority>): String {
        val builder = StringBuilder()
        val what = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I wish to report the following civic problem:")
        builder.appendLine("Incident: $what")
        builder.appendLine("Place: ${report.address.ifBlank { "not available" }}")
        if (report.city.isNotBlank() || report.district.isNotBlank() || report.state.isNotBlank()) {
            builder.appendLine(
                "Area: ${listOf(report.city, report.district, report.state).filter { it.isNotBlank() }.joinToString(", ")}"
            )
        }
        if (report.pincode.isNotBlank()) builder.appendLine("PIN code: ${report.pincode}")
        builder.appendLine(
            String.format(Locale.US, "GPS coordinates: %.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine(
            "Map: https://www.openstreetmap.org/?mlat=${report.latitude}&mlon=${report.longitude}#map=17/${report.latitude}/${report.longitude}"
        )
        builder.appendLine(
            "Recorded on: " + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(report.timestamp))
        )
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: ${report.description}")
        }
        builder.appendLine()
        builder.appendLine(
            "Photo/video evidence is attached to this complaint (${report.mediaType})."
        )
        builder.appendLine()
        builder.appendLine("Addressed to:")
        authorities.forEach { builder.appendLine("- ${it.name} (${it.level})") }
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the necessary action at the earliest.")
        builder.appendLine()
        builder.appendLine("Thanking you,")
        builder.appendLine("A concerned citizen (sent through the DuarBagori Reports app)")
        return builder.toString()
    }

    private fun cleanEmail(email: String): String? {
        val cleaned = email.trim()
        return if (cleaned.contains("@") && cleaned.contains(".")) cleaned else null
    }

    private fun cleanPhone(phone: String): String? {
        val digits = phone.filter { it.isDigit() || it == '+' }
        return if (digits.count { it.isDigit() } >= 4) digits else null
    }
}
