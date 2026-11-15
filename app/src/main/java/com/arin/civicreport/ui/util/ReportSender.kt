package com.arin.civicreport.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Composes the complaint text and hands it to the phone's email or SMS app. */
object ReportSender {

    fun buildSubject(report: Report): String {
        val kinds = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return "Civic complaint: $kinds at ${report.address.ifBlank { "reported location" }}"
    }

    fun buildBody(report: Report): String {
        val kinds = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val maps = String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
            report.latitude,
            report.longitude
        )
        val builder = StringBuilder()
        builder.appendLine("Sir / Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following civic issue:")
        builder.appendLine("Issue type: $kinds")
        builder.appendLine("Reported on: $whenText")
        builder.appendLine(
            String.format(Locale.US, "GPS location: %.6f, %.6f", report.latitude, report.longitude)
        )
        builder.appendLine("Map link: $maps")
        builder.appendLine("Address: ${report.address.ifBlank { "not available" }}")
        if (report.city.isNotBlank() || report.district.isNotBlank() || report.state.isNotBlank()) {
            builder.appendLine(
                "Area: ${report.city} ${report.district} ${report.state} ${report.pincode}".trim()
            )
        }
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Details: ${report.description}")
        }
        builder.appendLine()
        builder.appendLine(
            "A ${if (report.mediaType == "video") "video" else "photo"} of the spot was attached to this complaint from the DuarBagori Reports app."
        )
        builder.appendLine()
        builder.appendLine("Kindly inspect the location and take action.")
        builder.appendLine()
        builder.appendLine("Regards,")
        builder.appendLine("A concerned citizen")
        return builder.toString()
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val sendEmail = method == "email" || method == "both"
        val sendSms = method == "sms" || method == "both"

        val emails = authorities
            .map { it.email.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val phones = authorities
            .map { authority -> authority.phone.filter { it.isDigit() || it == '+' } }
            .filter { it.isNotBlank() }
            .distinct()

        if (sendEmail && emails.isNotEmpty()) {
            sendEmailIntent(context, emails, buildSubject(report), buildBody(report), report)
        }
        if (sendSms && phones.isNotEmpty()) {
            sendSmsIntent(context, phones, buildBody(report))
        }
    }

    private fun sendEmailIntent(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ) {
        val mediaUri = report.mediaUris.firstOrNull()?.let { runCatching { Uri.parse(it) }.getOrNull() }
        try {
            if (mediaUri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (report.mediaType == "video") "video/*" else "image/*"
                    putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, mediaUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Send complaint by email")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                val intent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + emails.joinToString(","))
                ).apply {
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Send complaint by email")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } catch (e: Exception) {
            runCatching {
                val fallback = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + emails.first())
                )
                context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun sendSmsIntent(context: Context, phones: List<String>, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("sms:" + phones.joinToString(","))
                putExtra("sms_body", body)
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            runCatching {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phones.first()))
                fallback.putExtra("sms_body", body)
                context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
