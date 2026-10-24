package com.loadtracker.pro

internal enum class TrackingNotificationAction(val titleResource: Int) {
    ARRIVE_SHIPPER(R.string.notification_arrive_shipper),
    DEPART_SHIPPER(R.string.notification_depart_shipper),
    ARRIVE_CONSIGNEE(R.string.notification_arrive_consignee),
    OPEN_TO_COMPLETE(R.string.notification_open_to_complete);

    companion object {
        fun forTripState(tripState: String): TrackingNotificationAction? =
            when (tripState) {
                "ACTIVE_BOUNCE" -> ARRIVE_SHIPPER
                "ACTIVE_SHIPPER" -> DEPART_SHIPPER
                "ACTIVE_LOADED" -> ARRIVE_CONSIGNEE
                "ACTIVE_CONSIGNEE" -> OPEN_TO_COMPLETE
                else -> null
            }
    }
}
