package com.kevin.astra.core.time

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    NSTimeZone.localTimeZone
        .secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1_000.0))
        .toInt()
