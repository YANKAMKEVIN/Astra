package com.kevin.astra.core.time

import java.util.TimeZone

actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis) / 1_000
