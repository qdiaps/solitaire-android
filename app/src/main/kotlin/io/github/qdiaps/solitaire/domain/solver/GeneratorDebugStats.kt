package io.github.qdiaps.solitaire.domain.solver

import kotlinx.serialization.Serializable

/**
 * Live telemetry metrics emitted by [DealGenerator] for developer diagnostics and monitoring.
 */
@Serializable
data class GeneratorDebugStats(
    val easyBankCount: Int = 0,
    val mediumBankCount: Int = 0,
    val activeWorkersCount: Int = 0,
    val totalCandidatesEvaluated: Long = 0L,
    val totalSolvableFound: Long = 0L,
    val rejectionRate: Float = 0f,
    val lastSolveDurationMs: Long = 0L
)
