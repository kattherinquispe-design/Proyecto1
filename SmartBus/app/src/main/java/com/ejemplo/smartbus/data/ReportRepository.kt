package com.ejemplo.smartbus.data

import android.content.Context
import com.ejemplo.smartbus.data.model.Report
import com.ejemplo.smartbus.data.model.ReportStatus
import org.json.JSONArray
import org.json.JSONObject

class ReportRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        "smartbus_reports",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_REPORTS = "reports_list"
    }

    fun addReport(report: Report) {
        val reports = getReports().toMutableList()
        reports.add(0, report)
        saveReports(reports)
    }

    fun updateReport(report: Report) {
        val reports = getReports().toMutableList()
        val index = reports.indexOfFirst { it.id == report.id }
        if (index >= 0) {
            reports[index] = report
            saveReports(reports)
        }
    }

    fun getReports(): List<Report> {
        val jsonString = preferences.getString(KEY_REPORTS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val reports = mutableListOf<Report>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                reports.add(
                    Report(
                        id = item.optLong("id", System.currentTimeMillis()),
                        userId = item.optString("userId", ""),
                        type = item.optString("type", "Otro"),
                        description = item.optString("description", ""),
                        busId = if (item.has("busId") && !item.isNull("busId")) item.getInt("busId") else null,
                        routeId = if (item.has("routeId") && !item.isNull("routeId")) item.getInt("routeId") else null,
                        stopId = if (item.has("stopId") && !item.isNull("stopId")) item.getInt("stopId") else null,
                        stopName = if (item.has("stopName") && !item.isNull("stopName")) item.getString("stopName") else null,
                        latitude = if (item.has("latitude") && !item.isNull("latitude")) item.getDouble("latitude") else null,
                        longitude = if (item.has("longitude") && !item.isNull("longitude")) item.getDouble("longitude") else null,
                        photoUri = if (item.has("photoUri") && !item.isNull("photoUri")) item.getString("photoUri") else null,
                        evidenceUri = if (item.has("evidenceUri") && !item.isNull("evidenceUri")) item.getString("evidenceUri") else null,
                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                        status = item.optString("status", ReportStatus.ENVIADO),
                        rating = if (item.has("rating") && !item.isNull("rating")) item.getInt("rating") else null
                    )
                )
            }
            reports
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveReports(reports: List<Report>) {
        val jsonArray = JSONArray()
        reports.forEach { report ->
            val jsonObject = JSONObject()
            jsonObject.put("id", report.id)
            jsonObject.put("userId", report.userId)
            jsonObject.put("type", report.type)
            jsonObject.put("description", report.description)
            jsonObject.put("busId", report.busId)
            jsonObject.put("routeId", report.routeId)
            jsonObject.put("stopId", report.stopId)
            jsonObject.put("stopName", report.stopName)
            jsonObject.put("latitude", report.latitude)
            jsonObject.put("longitude", report.longitude)
            jsonObject.put("photoUri", report.photoUri)
            jsonObject.put("evidenceUri", report.evidenceUri)
            jsonObject.put("timestamp", report.timestamp)
            jsonObject.put("status", report.status)
            jsonObject.put("rating", report.rating)
            jsonArray.put(jsonObject)
        }
        preferences.edit().putString(KEY_REPORTS, jsonArray.toString()).apply()
    }

    fun clearReports() {
        preferences.edit().clear().apply()
    }
}