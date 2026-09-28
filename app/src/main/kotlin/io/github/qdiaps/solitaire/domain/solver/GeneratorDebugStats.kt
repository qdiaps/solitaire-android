package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.serialization.Serializable

/**
 * An individual seed discovery event logged by a background solver worker.
 */
@Serializable
data class WorkerLogEntry(
    val timestampMs: Long = 0L,
    val workerId: Int = 1,
    val seed: Long = 0L,
    val difficulty: DealDifficulty = DealDifficulty.EASY,
    val durationMs: Long = 0L
)

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
    val lastSolveDurationMs: Long = 0L,
    val recentWorkerLogs: List<WorkerLogEntry> = emptyList()
)
