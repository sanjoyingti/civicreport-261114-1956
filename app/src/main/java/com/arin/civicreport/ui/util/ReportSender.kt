package com.arin.civicreport.ui.util

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
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

/**
 * Builds and launches the email / SMS compose screens for a finished report.
 * Nothing is uploaded anywhere: the phone's own mail and messaging apps do the sending.
 */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        if (method == "email" || method == "both") sendEmail(context, authorities, report)
        if (method == "sms" || method == "both") sendSms(context, authorities, report)
    }

    private fun subject(report: Report): String =
        "Civic complaint: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    private fun body(report: Report): String {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following issue:")
        builder.appendLine()
        builder.appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Date & time of capture: $stamp")
        builder.appendLine("Address: " + report.address.ifBlank { "not available" })
        if (report.district.isNotBlank()) builder.appendLine("District: " + report.district)
        if (report.state.isNotBlank()) builder.appendLine("State: " + report.state)
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: " + report.pincode)
        builder.appendLine(
            String.format(Locale.US, "GPS coordinates: %.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine(
            "Map: https://maps.google.com/?q=" + report.latitude + "," + report.longitude
        )
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: " + report.description)
        }
        builder.appendLine()
        builder.appendLine("The photo/video proof is attached. Kindly inspect the spot and take action.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.append("Filed with DuarBagori Reports")
        return builder.toString()
    }

    private fun smsText(report: Report): String {
        val titles = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val where = report.address.ifBlank {
            String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude)
        }
        return "DuarBagori Report: $titles at $where. Map: https://maps.google.com/?q=" +
            report.latitude + "," + report.longitude
    }

    private fun mime(report: Report): String =
        if (report.mediaType == "video") "video/*" else "image/*"

    private fun sendEmail(context: Context, authorities: List<Authority>, report: Report) {
        val emails = authorities.mapNotNull { it.email.trim().takeIf(String::isNotEmpty) }
        if (emails.isEmpty()) {
            toast(context, "The selected offices have no email address saved.")
            return
        }
        val attachments = report.mediaUris.mapNotNull { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()?.takeIf { it.scheme != null }
        }
        val action = if (attachments.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        val intent = Intent(action).apply {
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject(report))
            putExtra(Intent.EXTRA_TEXT, body(report))
            when {
                attachments.size == 1 -> {
                    putExtra(Intent.EXTRA_STREAM, attachments.first())
                    type = mime(report)
                }
                attachments.size > 1 -> {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
                    type = "*/*"
                }
                else -> type = "message/rfc822"
            }
            if (attachments.isNotEmpty()) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(
                Intent.createChooser(intent, "Send complaint by email").apply {
                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            toast(context, "No email app found on this device.")
        }
    }

    private fun sendSms(context: Context, authorities: List<Authority>, report: Report) {
        val numbers = authorities.mapNotNull { it.phone.trim().takeIf(String::isNotEmpty) }
        if (numbers.isEmpty()) {
            toast(context, "The selected offices have no phone number saved.")
            return
        }
        val text = smsText(report)
        val canSend = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (canSend) {
            try {
                val manager = context.getSystemService(SmsManager::class.java)
                if (manager != null) {
                    numbers.forEach { number ->
                        val parts = manager.divideMessage(text)
                        manager.sendMultipartTextMessage(number, null, parts, null, null)
                    }
                    toast(context, "SMS queued for ${numbers.size} office(s).")
                    return
                }
            } catch (e: Exception) {
                // fall through to the messaging app
            }
        }
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:" + numbers.joinToString(","))
        ).apply {
            putExtra("sms_body", text)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, "No messaging app found on this device.")
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
