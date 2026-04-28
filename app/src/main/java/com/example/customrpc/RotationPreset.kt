package com.example.customrpc

import org.json.JSONObject

data class RotationPreset(
    val label: String,
    val appId: String,
    val name: String,
    val details: String,
    val state: String,
    val activityType: Int = 0,
    val largeImageKey: String = "",
    val largeImageText: String = "",
    val smallImageKey: String = "",
    val smallImageText: String = "",
    val streamUrl: String = "",
    val button1Label: String = "",
    val button1Url: String = "",
    val button2Label: String = "",
    val button2Url: String = "",
    val userStatus: String = "online"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("label", label)
        put("appId", appId)
        put("name", name)
        put("details", details)
        put("state", state)
        put("activityType", activityType)
        put("largeImageKey", largeImageKey)
        put("largeImageText", largeImageText)
        put("smallImageKey", smallImageKey)
        put("smallImageText", smallImageText)
        put("streamUrl", streamUrl)
        put("button1Label", button1Label)
        put("button1Url", button1Url)
        put("button2Label", button2Label)
        put("button2Url", button2Url)
        put("userStatus", userStatus)
    }

    fun toPresenceData(): PresenceData = PresenceData(
        appId = appId,
        name = name,
        details = details,
        state = state,
        activityType = activityType,
        largeImageKey = largeImageKey,
        largeImageText = largeImageText,
        smallImageKey = smallImageKey,
        smallImageText = smallImageText,
        streamUrl = streamUrl,
        partySize = null,
        partyMax = null,
        partyId = null,
        button1Label = button1Label,
        button1Url = button1Url,
        button2Label = button2Label,
        button2Url = button2Url,
        userStatus = userStatus,
        timestampStart = System.currentTimeMillis(),
        timestampEnd = null
    )

    companion object {
        fun fromJson(obj: JSONObject) = RotationPreset(
            label = obj.optString("label", "Preset"),
            appId = obj.optString("appId"),
            name = obj.optString("name"),
            details = obj.optString("details"),
            state = obj.optString("state"),
            activityType = obj.optInt("activityType", 0),
            largeImageKey = obj.optString("largeImageKey"),
            largeImageText = obj.optString("largeImageText"),
            smallImageKey = obj.optString("smallImageKey"),
            smallImageText = obj.optString("smallImageText"),
            streamUrl = obj.optString("streamUrl"),
            button1Label = obj.optString("button1Label"),
            button1Url = obj.optString("button1Url"),
            button2Label = obj.optString("button2Label"),
            button2Url = obj.optString("button2Url"),
            userStatus = obj.optString("userStatus", "online")
        )
    }
}
