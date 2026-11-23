package com.snapcabin.camera

/**
 * Sentinel camera id meaning "use whatever external (USB) camera is connected".
 * More robust than a specific Camera2 id: a USB camera can come back under a
 * different id after a replug.
 */
const val EXTERNAL_CAMERA_ID = "external"

/** One camera as either the system camera service or CameraX currently lists it. */
internal data class KnownCamera(val id: String, val isExternal: Boolean)

/** Why the booth is not on the camera the operator selected. */
internal enum class FallbackReason {
    /** No USB camera is plugged in. */
    NOT_CONNECTED,

    /** A USB video device is on the bus, but the tablet isn't offering it as a camera. */
    NOT_OFFERED_BY_TABLET,

    /** Android won't open a USB camera without the microphone permission. */
    MIC_NOT_ALLOWED,

    /** The USB camera kept failing, so it is being skipped for a few minutes. */
    PAUSED_AFTER_FAILURES,

    /** The camera is listed, but the camera library can't use it. */
    NOT_USABLE
}

/** The outcome of [planCamera]: which camera to bind, and why if it isn't the requested one. */
internal data class CameraPlan(
    /** Camera to bind by id, or null for the built-in default (front/back toggle). */
    val cameraId: String? = null,
    /** The request is satisfied by any external camera, not one specific id. */
    val acceptAnyExternal: Boolean = false,
    /**
     * CameraX hasn't seen this camera (it was plugged in after CameraX started):
     * restart CameraX, then plan again.
     */
    val refreshFor: String? = null,
    val fallback: FallbackReason? = null
)

/**
 * Decide which camera to bind. Pure, so the hot-plug cases are unit-tested.
 *
 * Two lists disagree in practice, and each failure mode is silent:
 *  - CameraX (this version) reads the camera list ONCE, when it starts. A USB
 *    camera plugged in later is in [system] but not [provider] — binding it
 *    needs a CameraX restart ([CameraPlan.refreshFor]).
 *  - After an unplug the camera lingers in [provider]. Binding that phantom
 *    "succeeds" and shows a black preview, so a camera only [provider] knows is
 *    used only while a USB video device is physically present ([usbVideoPresent]).
 *
 * @param requestedId "" = automatic, [EXTERNAL_CAMERA_ID] = any external
 *   camera, otherwise a Camera2 id.
 * @param refreshTried cameras a CameraX restart was already attempted for.
 */
internal fun planCamera(
    requestedId: String,
    system: List<KnownCamera>,
    provider: List<KnownCamera>,
    usbVideoPresent: Boolean,
    micGranted: Boolean,
    externalPaused: Boolean,
    refreshTried: Set<String>
): CameraPlan {
    if (requestedId.isEmpty()) return CameraPlan()

    val providerIds = provider.map { it.id }.toSet()
    val isBuiltIn = (system + provider).any { it.id == requestedId && !it.isExternal }
    if (isBuiltIn) {
        return if (requestedId in providerIds) {
            CameraPlan(cameraId = requestedId)
        } else {
            CameraPlan(fallback = FallbackReason.NOT_USABLE)
        }
    }

    // Built-in cameras never disappear, so an id the system no longer lists
    // was a USB camera: it may be back under a new id, and any external
    // camera is the one the operator meant.
    val acceptAny = requestedId == EXTERNAL_CAMERA_ID || system.none { it.id == requestedId }

    // Some tablets leave a working USB camera out of the system list; trust
    // CameraX's copy only then, and only while the device is plugged in.
    var externals = system.filter { it.isExternal }.map { it.id }
    if (externals.isEmpty() && usbVideoPresent) {
        externals = provider.filter { it.isExternal }.map { it.id }
    }
    val candidates = if (acceptAny) {
        externals.sortedByDescending { it == requestedId } // the exact id first, if it's there
    } else {
        listOf(requestedId)
    }

    if (candidates.isEmpty()) {
        return CameraPlan(
            acceptAnyExternal = acceptAny,
            fallback = if (usbVideoPresent) {
                FallbackReason.NOT_OFFERED_BY_TABLET
            } else {
                FallbackReason.NOT_CONNECTED
            }
        )
    }
    if (!micGranted) {
        return CameraPlan(acceptAnyExternal = acceptAny, fallback = FallbackReason.MIC_NOT_ALLOWED)
    }
    if (externalPaused) {
        return CameraPlan(acceptAnyExternal = acceptAny, fallback = FallbackReason.PAUSED_AFTER_FAILURES)
    }

    candidates.firstOrNull { it in providerIds }?.let {
        return CameraPlan(cameraId = it, acceptAnyExternal = acceptAny)
    }
    candidates.firstOrNull { it !in refreshTried }?.let {
        return CameraPlan(acceptAnyExternal = acceptAny, refreshFor = it)
    }
    return CameraPlan(acceptAnyExternal = acceptAny, fallback = FallbackReason.NOT_USABLE)
}
