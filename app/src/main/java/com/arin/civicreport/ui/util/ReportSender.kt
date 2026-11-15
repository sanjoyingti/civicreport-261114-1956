package com.arin.civicreport.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.arin.civicreport.R
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Opens the phone's email / SMS app pre-filled with the complaint and the media attachment. */
object ReportSender {

    private const val CHANNEL_ID = "report_sent"

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email }.filter { it.isNotBlank() }.distinct()
        val phones = authorities.map { it.phone }.filter { it.isNotBlank() }.distinct()
        val subject = buildSubject(report)
        val body = buildBody(report, authorities)

        when (method) {
            "email" -> sendEmail(context, emails, subject, body, report)
            "sms" -> sendSms(context, phones, body)
            else -> {
                sendEmail(context, emails, subject, body, report)
                sendSms(context, phones, body)
            }
        }
        notifySent(context, subject)
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ) {
        if (emails.isEmpty()) {
            Toast.makeText(context, "No email address saved for the chosen offices", Toast.LENGTH_LONG).show()
            return
        }
        val mediaUri = report.mediaUris.firstOrNull()
        val type = if (report.mediaType == "video") "video/*" else "image/*"
        val intent = Intent(if (mediaUri != null) Intent.ACTION_SEND else Intent.ACTION_SENDTO).apply {
            if (mediaUri != null) {
                this.type = type
            } else {
                data = Uri.parse("mailto:" + emails.joinToString(","))
            }
        }
        intent.putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        if (mediaUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUri))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Send complaint by email")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        if (phones.isEmpty()) {
            Toast.makeText(context, "No phone number saved for the chosen offices", Toast.LENGTH_LONG).show()
            return
        }
        val numbers = phones.joinToString(",") { it.replace(" ", "") }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numbers"))
        intent.putExtra("sms_body", body)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No messaging app found on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildSubject(report: Report): String {
        val what = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Complaint: $what at ${report.address.ifBlank { "reported spot" }}"
    }

    private fun buildBody(report: Report, authorities: List<Authority>): String {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("Dear Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following civic problem:")
        builder.appendLine()
        builder.appendLine("Issue: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
        builder.appendLine("Address: " + report.address.ifBlank { "see GPS coordinates below" })
        builder.appendLine("GPS: " + String.format(Locale.US, "%.6f, %.6f", report.latitude, report.longitude))
        if (report.city.isNotBlank()) builder.appendLine("City: " + report.city)
        if (report.district.isNotBlank()) builder.appendLine("District: " + report.district)
        if (report.state.isNotBlank()) builder.appendLine("State: " + report.state)
        if (report.pincode.isNotBlank()) builder.appendLine("PIN: " + report.pincode)
        builder.appendLine("Reported on: $stamp")
        builder.appendLine("Evidence: " + (if (report.mediaUris.isEmpty()) "no media" else "one " + report.mediaType + " attached"))
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Notes: " + report.description)
        }
        builder.appendLine()
        builder.appendLine("Requested action: please inspect the spot and take corrective action at the earliest.")
        builder.appendLine()
        builder.appendLine("Copied to: " + authorities.joinToString(", ") { it.name })
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.append("A concerned citizen (via DuarBagori Reports)")
        return builder.toString()
    }

    private fun notifySent(context: Context, subject: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Report confirmations",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle("Complaint ready to send")
                .setContentText(subject)
                .setAutoCancel(true)
                .build()
            manager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            // notifications are best effort only
        }
    }
}
