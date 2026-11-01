package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CookingPreset
import com.example.model.CookingSession
import com.example.model.DefaultPresets
import com.example.model.WhistleRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CookingRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("whistle_cooking_data", Context.MODE_PRIVATE)

    private val _presets = MutableStateFlow<List<CookingPreset>>(emptyList())
    val presets: StateFlow<List<CookingPreset>> = _presets.asStateFlow()

    private val _history = MutableStateFlow<List<CookingSession>>(emptyList())
    val history: StateFlow<List<CookingSession>> = _history.asStateFlow()

    init {
        loadPresets()
        loadHistory()
    }

    private fun loadPresets() {
        val savedCustom = prefs.getString("custom_presets_json", null)
        val customList = mutableListOf<CookingPreset>()
        if (!savedCustom.isNullOrEmpty()) {
            try {
                val array = JSONArray(savedCustom)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customList.add(
                        CookingPreset(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            category = obj.optString("category", "Custom"),
                            defaultWhistles = obj.getInt("defaultWhistles"),
                            flameAdvice = obj.optString("flameAdvice", "Standard cooking"),
                            tips = obj.optString("tips", "")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _presets.value = DefaultPresets.list + customList
    }

    fun addCustomPreset(preset: CookingPreset) {
        val currentList = _presets.value.toMutableList()
        currentList.add(preset)
        _presets.value = currentList

        // Save custom presets
        val customOnly = currentList.filter { item -> DefaultPresets.list.none { it.id == item.id } }
        val array = JSONArray()
        for (item in customOnly) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("category", item.category)
                put("defaultWhistles", item.defaultWhistles)
                put("flameAdvice", item.flameAdvice)
                put("tips", item.tips)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_presets_json", array.toString()).apply()
    }

    private fun loadHistory() {
        val savedHistory = prefs.getString("cooking_history_json", null)
        val sessions = mutableListOf<CookingSession>()
        if (!savedHistory.isNullOrEmpty()) {
            try {
                val array = JSONArray(savedHistory)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val whistlesArr = obj.optJSONArray("whistles")
                    val whistleRecords = mutableListOf<WhistleRecord>()
                    if (whistlesArr != null) {
                        for (w in 0 until whistlesArr.length()) {
                            val wObj = whistlesArr.getJSONObject(w)
                            whistleRecords.add(
                                WhistleRecord(
                                    whistleNumber = wObj.getInt("whistleNumber"),
                                    timestampMs = wObj.getLong("timestampMs"),
                                    durationSec = wObj.getDouble("durationSec").toFloat(),
                                    intervalSinceLastSec = wObj.optLong("intervalSinceLastSec", 0L)
                                )
                            )
                        }
                    }

                    sessions.add(
                        CookingSession(
                            id = obj.getString("id"),
                            dishName = obj.getString("dishName"),
                            targetWhistles = obj.getInt("targetWhistles"),
                            actualWhistles = obj.getInt("actualWhistles"),
                            startTimeMs = obj.getLong("startTimeMs"),
                            endTimeMs = obj.getLong("endTimeMs"),
                            whistles = whistleRecords,
                            completedSuccessfully = obj.optBoolean("completedSuccessfully", true)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _history.value = sessions.sortedByDescending { it.startTimeMs }
    }

    fun saveCompletedSession(session: CookingSession) {
        val current = _history.value.toMutableList()
        current.add(0, session)
        _history.value = current

        // Keep last 30 sessions
        val toSave = current.take(30)
        val array = JSONArray()
        for (s in toSave) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("dishName", s.dishName)
                put("targetWhistles", s.targetWhistles)
                put("actualWhistles", s.actualWhistles)
                put("startTimeMs", s.startTimeMs)
                put("endTimeMs", s.endTimeMs)
                put("completedSuccessfully", s.completedSuccessfully)

                val wArr = JSONArray()
                for (w in s.whistles) {
                    val wObj = JSONObject().apply {
                        put("whistleNumber", w.whistleNumber)
                        put("timestampMs", w.timestampMs)
                        put("durationSec", w.durationSec.toDouble())
                        put("intervalSinceLastSec", w.intervalSinceLastSec)
                    }
                    wArr.put(wObj)
                }
                put("whistles", wArr)
            }
            array.put(sObj)
        }
        prefs.edit().putString("cooking_history_json", array.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("cooking_history_json").apply()
        _history.value = emptyList()
    }
}
