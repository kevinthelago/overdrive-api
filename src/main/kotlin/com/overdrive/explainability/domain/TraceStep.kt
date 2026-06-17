package com.overdrive.explainability.domain

data class TraceStep(
    val step: Int,
    val description: String,
    val inputs: Map<String, Any>,
    val outputs: Map<String, Any>,
    val rule: String? = null
)
