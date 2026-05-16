package com.snapcabin.ui.screens.capture

enum class CaptureMode(val routeArg: String) {
    Single("single"),
    Collage("collage"),
    Gif("gif");

    companion object {
        fun fromArg(s: String?): CaptureMode = entries.firstOrNull { it.routeArg == s } ?: Single
    }
}
