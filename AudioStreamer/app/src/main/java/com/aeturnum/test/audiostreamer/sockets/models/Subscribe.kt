package com.aeturnum.test.audiostreamer.sockets.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class Subscribe(val type: String = "subscribe",
                     @Json(name = "item_type")
                     val itemType: String,
                     val channels: List<String>,)
