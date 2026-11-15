package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the complaint text and hands it to the phone's email or SMS app.
 * Everything is pre-filled; the user only has to press send in that app.
 */
object ReportSender {

    private const val TAG = "CivicReport"

    fun composeMessage(report: Report): String {
        val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("CIVIC COMPLAINT filed via DuarBagori Reports")
        builder.appendLine()
        builder.appendLine(
            "Incident: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        )
        builder.appendLine("Filed on: $whenText")
        builder.appendLine("Address: " + report.address.ifBlank { "unknown" })
        builder.appendLine(
            String.format(Locale.US, "GPS location: %.6f, %.6f", report.latitude, report.longitude)
        )
        val area = listOf(report.city, report.district, report.state, report.pincode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (area.isNotBlank()) builder.appendLine("Area: $area")
        builder.appendLine(
            "Attachment: " + report.mediaType.ifBlank { "photo" } +
                " (" + report.mediaUris.size + " file)"
        )
        builder.appendLine("Local reference: #" + report.id)
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Notes: " + report.description)
        }
        builder.appendLine()
        builder.append(
            "Requested action: please inspect the spot at the coordinates above and take " +
                "corrective action at the earliest. Photographs and videos are attached."
        )
        return builder.toString()
    }

    fun composeSubject(report: Report): String {
        val first = report.incidentTypes.firstOrNull()
        val title = if (first != null) IncidentType.titleFor(first) else "Civic issue"
        val place = report.city.ifBlank { report.state }.ifBlank { report.address }
        return "Complaint: $title" + if (place.isNotBlank()) " at $place" else ""
    }

    /** Sends the complaint to every authority that has a usable address for the chosen method. */
    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email.trim() }.filter { it.isNotBlank() }
        val phones = authorities.map { it.phone.trim() }.filter { it.isNotBlank() }
        val message = composeMessage(report)

        if (method == "email" || method == "both") {
            if (emails.isNotEmpty()) sendEmail(context, emails, composeSubject(report), message, report)
            else if (method == "email") toast(context, "No email address on the selected offices.")
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty()) sendSms(context, phones, message)
            else if (method == "sms") toast(context, "No phone number on the selected offices.")
        }
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        message: String,
        report: Report
    ) {
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:" + emails.joinToString(","))
        ).apply {
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val attachments = report.mediaUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        if (attachments.isNotEmpty()) {
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
            intent.type = if (report.mediaType == "video") "video/*" else "image/*"
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send complaint by email"))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No email app", e)
            toast(context, "No email app is installed on this phone.")
        } catch (e: Exception) {
            Log.e(TAG, "Email failed", e)
            toast(context, "The email app could not be opened.")
        }
    }

    private fun sendSms(context: Context, phones: List<String>, message: String) {
        val numbers = phones.map { it.replace(" ", "").replace("-", "") }
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:" + numbers.joinToString(","))
        ).apply {
            putExtra("sms_body", message)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send complaint by SMS"))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No SMS app", e)
            toast(context, "No messaging app is installed on this phone.")
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed", e)
            toast(context, "The messaging app could not be opened.")
        }
    }

    fun call(context: Context, phone: String) {
        val cleaned = phone.replace(" ", "").replace("-", "")
        if (cleaned.isBlank()) {
            toast(context, "This office has no phone number saved.")
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
        } catch (e: Exception) {
            Log.e(TAG, "Dial failed", e)
            toast(context, "The dialler could not be opened.")
        }
    }

    fun emailTo(context: Context, authority: Authority, report: Report) {
        if (authority.email.isBlank()) {
            toast(context, "This office has no email address saved.")
            return
        }
        sendEmail(
            context = context,
            emails = listOf(authority.email.trim()),
            subject = composeSubject(report),
            message = composeMessage(report),
            report = report
        )
    }

    private fun toast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}
