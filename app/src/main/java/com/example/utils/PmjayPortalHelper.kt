package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object PmjayPortalHelper {

    const val PMJAY_HOSPITAL_SEARCH_URL = "https://hem.nha.gov.in/search"

    fun buildSearchIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(PMJAY_HOSPITAL_SEARCH_URL)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openNearPmjayHospitalSearch(context: Context): Boolean {
        return try {
            val intent = buildSearchIntent()
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            try {
                val chooser = Intent.createChooser(buildSearchIntent(), "NEAR PMJAY HOSPITAL FIND").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Throwable) {
                Toast.makeText(
                    context,
                    "Unable to open browser. Visit: $PMJAY_HOSPITAL_SEARCH_URL",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }
}
