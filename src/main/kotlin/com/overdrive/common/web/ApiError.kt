package com.overdrive.common.web

/**
 * Shared error contract for frontend consumers.
 * Mirrors the RFC 7807 problem+json shape emitted by [GlobalExceptionHandler].
 */
data class ApiError(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val errors: List<FieldError>? = null,
    val reason: String? = null,
) {
    data class FieldError(
        val field: String,
        val message: String,
    )
}
