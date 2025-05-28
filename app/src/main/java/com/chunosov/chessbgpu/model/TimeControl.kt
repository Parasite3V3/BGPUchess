package com.chunosov.chessbgpu.model

enum class TimeControl(val minutes: Int) {
    NoLimit(0),
    OneMinute(1),
    FiveMinutes(5),
    TenMinutes(10)
} 