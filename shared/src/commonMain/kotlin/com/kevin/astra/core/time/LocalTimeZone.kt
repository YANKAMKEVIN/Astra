package com.kevin.astra.core.time

/** Offset of the phone's time zone from UTC at [epochMillis], in seconds (DST included). */
expect fun localUtcOffsetSeconds(epochMillis: Long): Int
