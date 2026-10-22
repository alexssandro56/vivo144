package com.example.data.models

enum class DiagnosticStatusType {
    SUPPORTED,
    CONNECTED,
    AVAILABLE_144,
    WARNING_REQ_ADB,
    GENERIC_FALLBACK,
    INFO
}

data class DiagnosticItem(
    val id: String,
    val title: String,
    val value: String,
    val statusType: DiagnosticStatusType,
    val statusLabel: String,
    val detail: String? = null
)
