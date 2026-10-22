package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class CompatibilityConfidence {
    OFFICIAL_VERIFIED,
    SERIES_COMPATIBLE,
    UNLISTED_DEVICE,
    NON_VIVO_GENERIC
}

data class GroundedSource(
    val title: String,
    val uri: String
)

data class SupportedDeviceEntry(
    val modelName: String,
    val maxRefreshRate: Int = 144,
    val series: String,
    val chipset: String = "",
    val notes: String = ""
)

data class GroundedCompatibilityReport(
    val isOfficiallyListed: Boolean,
    val confidence: CompatibilityConfidence,
    val deviceModelChecked: String,
    val summaryVerdict: String,
    val detailedAnalysis: String,
    val officialSupportedModels: List<SupportedDeviceEntry> = emptyList(),
    val groundedSources: List<GroundedSource> = emptyList(),
    val searchQueriesUsed: List<String> = emptyList(),
    val isLiveGrounded: Boolean = true,
    val timestampMs: Long = System.currentTimeMillis()
)

// Gemini API Request & Response Data Classes (Moshi compatible)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    val googleSearch: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null,
    val searchEntryPoint: GeminiSearchEntryPoint? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    val web: GeminiWebChunk? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebChunk(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSearchEntryPoint(
    val renderedContent: String? = null
)
