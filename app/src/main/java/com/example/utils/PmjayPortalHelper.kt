package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object PmjayPortalHelper {

    const val PMJAY_HOSPITAL_SEARCH_URL = "https://hem.nha.gov.in/search"

    fun resolvePortalUrl(urlOverride: String? = null): String {
        val candidate = urlOverride?.trim().orEmpty()
        return if (candidate.startsWith("https://", ignoreCase = true) ||
            candidate.startsWith("http://", ignoreCase = true)
        ) {
            candidate
        } else {
            PMJAY_HOSPITAL_SEARCH_URL
        }
    }

    fun buildSearchIntent(urlOverride: String? = null): Intent {
        val targetUrl = resolvePortalUrl(urlOverride)
        return Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openNearPmjayHospitalSearch(context: Context, urlOverride: String? = null): Boolean {
        val targetUrl = resolvePortalUrl(urlOverride)
        return try {
            val intent = buildSearchIntent(targetUrl)
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            try {
                val chooser = Intent.createChooser(buildSearchIntent(targetUrl), "NEAR PMJAY HOSPITAL FIND").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Throwable) {
                Toast.makeText(
                    context,
                    "Unable to open browser. Visit: $targetUrl",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }
}
