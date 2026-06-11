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
    val supportsEosRemote: Boolean
        get() = operationsSupported.contains(Ptp.OP_EOS_SET_REMOTE_MODE)
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
