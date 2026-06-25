package com.kevin.astra

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform