package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the complaint and hands it to the phone's email or SMS app. */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.mapNotNull { it.email.trim().takeIf(String::isNotEmpty) }.distinct()
        val phones = authorities.mapNotNull { it.phone.trim().takeIf(String::isNotEmpty) }.distinct()
        if (method != "sms" && emails.isNotEmpty()) {
            openEmail(context, emails, subjectFor(report), buildBody(report), report.mediaUris.firstOrNull())
        }
        if (method != "email" && phones.isNotEmpty()) {
            openSms(context, phones, buildBody(report))
        }
        if (method == "email" && emails.isEmpty()) {
            toast(context, "No email address for the chosen offices.")
        }
        if (method == "sms" && phones.isEmpty()) {
            toast(context, "No phone number for the chosen offices.")
        }
    }

    private fun subjectFor(report: Report): String {
        val what = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val where = report.address.ifBlank { "reported spot" }
        return "Civic complaint: $what at $where"
    }

    private fun buildBody(report: Report): String {
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        return buildString {
            appendLine("Respected Sir / Madam,")
            appendLine()
            appendLine("I would like to report the following civic problem:")
            appendLine("Problem: $types")
            appendLine("Address: ${report.address.ifBlank { "not available" }}")
            appendLine(
                String.format(
                    Locale.US,
                    "GPS coordinates: %.6f, %.6f",
                    report.latitude,
                    report.longitude
                )
            )
            if (report.city.isNotBlank() || report.district.isNotBlank() || report.state.isNotBlank()) {
                appendLine(
                    "Area: ${report.city} ${report.district} ${report.state} ${report.pincode}".trim()
                )
            }
            appendLine("Reported on: $whenText")
            appendLine("Media attached: ${report.mediaType.ifBlank { "none" }}")
            if (report.description.isNotBlank()) {
                appendLine()
                appendLine("Notes: ${report.description}")
            }
            appendLine()
            appendLine("Kindly inspect the spot and take action at the earliest.")
            appendLine()
            appendLine("Sent from DuarBagori Reports.")
        }
    }

    private fun openEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        media: String?
    ) {
        val intent = if (media.isNullOrBlank()) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emails.joinToString(","))).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeFor(media)
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        launch(context, intent, "No email app found. Install one or use SMS.")
    }

    private fun openSms(context: Context, phones: List<String>, body: String) {
        val uri = Uri.parse("smsto:" + phones.joinToString(","))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
        }
        launch(context, intent, "No SMS app found.")
    }

    private fun mimeFor(media: String): String =
        if (media.contains("video")) "video/mp4" else "image/jpeg"

    private fun launch(context: Context, intent: Intent, fallback: String) {
        try {
            val toStart = if (intent.action == Intent.ACTION_SEND) {
                Intent.createChooser(intent, "Send complaint")
            } else {
                intent
            }
            toStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(toStart)
        } catch (e: ActivityNotFoundException) {
            toast(context, fallback)
        } catch (e: Exception) {
            toast(context, fallback)
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
