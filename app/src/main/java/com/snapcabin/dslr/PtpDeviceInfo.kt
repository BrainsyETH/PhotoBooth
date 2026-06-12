package com.snapcabin.dslr

/**
 * Parsed result of GetDeviceInfo — enough to confirm WHICH camera we're talking
 * to and whether it speaks the Canon EOS extensions we'll need for live view /
 * remote capture in later milestones.
 */
data class PtpDeviceInfo(
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
    val vendorExtensionId: Long,
    val operationsSupported: IntArray
) {
    val isCanon: Boolean get() = vendorExtensionId.toInt() == Ptp.VENDOR_CANON

    /** Newer staged release (half/full press), 0x9128/0x9129. */
    val supportsStagedRelease: Boolean
        get() = operationsSupported.contains(Ptp.OP_EOS_REMOTE_RELEASE_ON)

    /** Classic one-shot release, 0x910F — the only one older Rebels have. */
    val supportsOneShotRelease: Boolean
        get() = operationsSupported.contains(Ptp.OP_EOS_REMOTE_RELEASE)

    /**
     * "Remote capture supported" must mean the ops CAPTURE actually uses — a
     * release op plus SetRemoteMode — not just SetRemoteMode. Older Rebels
     * advertise 0x9114 but not 0x9128, and gating on the wrong op made the UI
     * promise a capture the camera was always going to refuse.
     */
    val supportsEosRemote: Boolean
        get() = operationsSupported.contains(Ptp.OP_EOS_SET_REMOTE_MODE) &&
            (supportsStagedRelease || supportsOneShotRelease)

    val supportsEosLiveView: Boolean
        get() = operationsSupported.contains(Ptp.OP_EOS_GET_VIEWFINDER_DATA)

    companion object {
        /**
         * Parse the GetDeviceInfo dataset. Field order is fixed by the PTP spec:
         * StandardVersion, VendorExtensionID, VendorExtensionVersion,
         * VendorExtensionDesc, FunctionalMode, then five uint16 arrays
         * (OperationsSupported … ImageFormats), then Manufacturer, Model,
         * DeviceVersion, SerialNumber.
         */
        fun parse(data: ByteArray): PtpDeviceInfo {
            val r = PtpReader(data)
            r.u16()                       // StandardVersion
            val vendorId = r.u32()        // VendorExtensionID
            r.u16()                       // VendorExtensionVersion
            r.string()                    // VendorExtensionDesc
            r.u16()                       // FunctionalMode
            val ops = r.u16Array()        // OperationsSupported
            r.u16Array()                  // EventsSupported
            r.u16Array()                  // DevicePropertiesSupported
            r.u16Array()                  // CaptureFormats
            r.u16Array()                  // ImageFormats
            val manufacturer = r.string()
            val model = r.string()
            r.string()                    // DeviceVersion
            val serial = r.string()
            return PtpDeviceInfo(
                manufacturer = manufacturer,
                model = model,
                serialNumber = serial,
                vendorExtensionId = vendorId,
                operationsSupported = ops
            )
        }
    }
}
