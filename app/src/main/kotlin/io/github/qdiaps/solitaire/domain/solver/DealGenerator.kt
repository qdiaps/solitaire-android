package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Background deal generator maintaining buffers of pre-verified solvable Klondike deals
 * segmented by [DealDifficulty].
 *
 * Supports hybrid operation:
 * - If [seedBank] is present, instantly consumes pre-verified solvable seeds with zero latency (0 ms),
 *   triggering dynamic background replenishment when the bank drops below thresholds.
 * - Otherwise, operates as an in-memory channel-buffered producer with backpressure.
 *
 * @param scope Lifecycle-aware [CoroutineScope] managing background generation jobs.
 * @param dispatcher Dispatcher for CPU-intensive deal generation and solving (default: [Dispatchers.Default]).
 * @param bufferCapacity Number of pre-verified solvable deals kept ready in memory per difficulty (default: 3).
 * @param solverConfig Solver parameters used for checking generated deals.
 * @param seedBank Optional persistent rotating seed bank providing instant deals.
 * @param coreCountProvider Provider of available CPU cores for dynamic scaling calculations.
 * @param candidateSeedProvider Provider of candidate seeds for replenishment.
 * @param dealProvider Provider function generating candidate deals (default: [KlondikeDealer.dealShuffled]).
 * @param solvabilityChecker Checker function verifying deal solvability (default: [SolvabilityChecker.checkSolvability]).
 * @param difficultyClassifier Function classifying solvable deals into [DealDifficulty] (default: [DealDifficultyClassifier.classify]).
 */
class DealGenerator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY,
    val solverConfig: SolverConfig = SolverConfig(),
    val seedBank: PersistentSeedBank? = null,
    val coreCountProvider: () -> Int = { Runtime.getRuntime().availableProcessors() },
    private val candidateSeedProvider: () -> Long = { Random.nextLong(1L, Long.MAX_VALUE) },
    private val dealProvider: () -> BoardState = { KlondikeDealer.dealShuffled() },
    private val solvabilityChecker: (BoardState, SolverConfig) -> SolvabilityResult = { state, config ->
        SolvabilityChecker.checkSolvability(state, config)
    },
    private val difficultyClassifier: (BoardState, SolvabilityResult.Solvable) -> DealDifficulty = { state, result ->
        DealDifficultyClassifier.classify(state, result, solverConfig.drawMode) ?: DealDifficulty.MEDIUM
    }
) {
    companion object {
        const val DEFAULT_BUFFER_CAPACITY: Int = 3

        /**
         * Calculates the desired background worker count based on current bank count and available CPU cores:
         * - >= 90 seeds: 0 workers (idle)
         * - 50..89 seeds: 1 worker (routine single-threaded background top-up)
         * - < 50 seeds: max(2, min(cores - 1, 4)) workers (parallel fast replenishment)
         */
        fun calculateWorkerCount(currentCount: Int, coreCount: Int = Runtime.getRuntime().availableProcessors()): Int {
            if (currentCount >= PersistentSeedBank.REPLENISH_THRESHOLD) return 0
            if (currentCount >= PersistentSeedBank.DEEP_DEPLETION_THRESHOLD) return 1
            return maxOf(2, minOf(coreCount - 1, 4))
        }
    }

    private val easyChannel = Channel<BoardState>(capacity = bufferCapacity)
    private val mediumChannel = Channel<BoardState>(capacity = bufferCapacity)

    private var easyJob: Job? = null
    private var mediumJob: Job? = null
    private var seedBankCollectorJob: Job? = null

    private val activeWorkers = AtomicInteger(0)
    private val replenishmentJobs = ConcurrentHashMap<DealDifficulty, MutableList<Job>>()
    private val totalEvaluated = AtomicLong(0L)
    private val totalSolvable = AtomicLong(0L)
    private val statsLock = Any()

    private val _debugStats = MutableStateFlow(
        GeneratorDebugStats(
            easyBankCount = seedBank?.getAvailableCount(DealDifficulty.EASY) ?: 0,
            mediumBankCount = seedBank?.getAvailableCount(DealDifficulty.MEDIUM) ?: 0
        )
    )
    val debugStats: StateFlow<GeneratorDebugStats> = _debugStats.asStateFlow()

    init {
        require(bufferCapacity > 0) {
            "bufferCapacity must be greater than zero, got: $bufferCapacity"
        }
        if (seedBank == null) {
            start()
        } else {
            seedBankCollectorJob = scope.launch {
                seedBank.state.collect { state ->
                    synchronized(statsLock) {
                        _debugStats.value = _debugStats.value.copy(
                            easyBankCount = state.easySeeds.size,
                            mediumBankCount = state.mediumSeeds.size
                        )
                    }
                }
            }
        }
    }

    /**
     * Convenience constructor configuring [DealGenerator] with a specific [DrawMode].
     */
    constructor(
        scope: CoroutineScope,
        drawMode: DrawMode,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY,
        seedBank: PersistentSeedBank? = null
    ) : this(
        scope = scope,
        dispatcher = dispatcher,
        bufferCapacity = bufferCapacity,
        solverConfig = SolverConfig(drawMode = drawMode),
        seedBank = seedBank
    )

    /**
     * Starts background deal generation for the Easy buffer if not already active.
     */
    fun start() {
        startEasyProducer()
    }

    /**
     * Starts the producer coroutine generating [DealDifficulty.EASY] deals.
     */
    fun startEasyProducer() {
        if (easyJob?.isActive == true) return
        easyJob = scope.launch(dispatcher) {
            try {
                while (isActive) {
                    val candidate = dealProvider()
                    val result = solvabilityChecker(candidate, solverConfig)
                    if (result is SolvabilityResult.Solvable) {
                        val difficulty = difficultyClassifier(candidate, result)
                        if (difficulty == DealDifficulty.EASY) {
                            easyChannel.send(candidate)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    /**
     * Starts the producer coroutine generating [DealDifficulty.MEDIUM] deals.
     */
    fun startMediumProducer() {
        if (mediumJob?.isActive == true) return
        mediumJob = scope.launch(dispatcher) {
            try {
                while (isActive) {
                    val candidate = dealProvider()
                    val result = solvabilityChecker(candidate, solverConfig)
                    if (result is SolvabilityResult.Solvable) {
                        val difficulty = difficultyClassifier(candidate, result)
                        if (difficulty == DealDifficulty.MEDIUM) {
                            mediumChannel.send(candidate)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    /**
     * Checks current seed bank capacity for [difficulty] and adjusts replenishment workers dynamically.
     */
    fun checkAndReplenish(difficulty: DealDifficulty) {
        val bank = seedBank ?: return
        if (difficulty != DealDifficulty.EASY && difficulty != DealDifficulty.MEDIUM) return

        val currentCount = bank.getAvailableCount(difficulty)
        val desiredWorkers = calculateWorkerCount(currentCount, coreCountProvider())

        val currentJobs = replenishmentJobs.getOrPut(difficulty) { mutableListOf() }
        synchronized(currentJobs) {
            currentJobs.removeAll { !it.isActive }
            val runningCount = currentJobs.size

            if (desiredWorkers <= 0 || currentCount >= PersistentSeedBank.TARGET_CAPACITY) {
                currentJobs.forEach { it.cancel() }
                currentJobs.clear()
                return
            }

            val needed = desiredWorkers - runningCount
            if (needed > 0 && scope.isActive) {
                repeat(needed) {
                    val job = scope.launch(dispatcher) {
                        updateActiveWorkers(1)
                        try {
                            while (isActive && bank.getAvailableCount(difficulty) < PersistentSeedBank.TARGET_CAPACITY) {
                                val candidateSeed = candidateSeedProvider()
                                val board = KlondikeDealer.dealFromSeed(candidateSeed)
                                val startTime = System.currentTimeMillis()
                                val result = solvabilityChecker(board, solverConfig)
                                val duration = System.currentTimeMillis() - startTime

                                var foundSolvable = false
                                if (result is SolvabilityResult.Solvable) {
                                    val classified = difficultyClassifier(board, result)
                                    if (classified == difficulty) {
                                        if (bank.addSeed(difficulty, candidateSeed)) {
                                            foundSolvable = true
                                        }
                                    }
                                }

                                recordEvaluation(
                                    solvable = foundSolvable,
                                    durationMs = if (foundSolvable) duration else 0L
                                )
                            }
                        } finally {
                            updateActiveWorkers(-1)
                        }
                    }
                    currentJobs.add(job)
                }
            }
        }
    }

    private fun updateActiveWorkers(delta: Int) {
        val count = activeWorkers.addAndGet(delta).coerceAtLeast(0)
        synchronized(statsLock) {
            _debugStats.value = _debugStats.value.copy(activeWorkersCount = count)
        }
    }

    private fun recordEvaluation(solvable: Boolean, durationMs: Long) {
        val ev = totalEvaluated.incrementAndGet()
        val sol = if (solvable) totalSolvable.incrementAndGet() else totalSolvable.get()
        val rejected = (ev - sol).coerceAtLeast(0)
        val rate = if (ev > 0) rejected.toFloat() / ev.toFloat() else 0f

        synchronized(statsLock) {
            _debugStats.value = _debugStats.value.copy(
                totalCandidatesEvaluated = ev,
                totalSolvableFound = sol,
                rejectionRate = rate,
                lastSolveDurationMs = if (durationMs > 0) durationMs else _debugStats.value.lastSolveDurationMs
            )
        }
    }

    /**
     * Stops all background generator jobs.
     */
    fun stop() {
        seedBankCollectorJob?.cancel()
        seedBankCollectorJob = null
        easyJob?.cancel()
        easyJob = null
        mediumJob?.cancel()
        mediumJob = null
        replenishmentJobs.values.forEach { list ->
            synchronized(list) {
                list.forEach { it.cancel() }
                list.clear()
            }
        }
    }

    /**
     * Retrieves the next pre-verified deal matching the requested [difficulty].
     *
     * - [DealDifficulty.RANDOM]: bypasses the solver and instantly returns a freshly shuffled deal.
     * - [DealDifficulty.EASY], [DealDifficulty.MEDIUM]:
     *   - If [seedBank] is present: consumes a seed without replacement and returns deterministic deal.
     *   - Otherwise: retrieves from respective buffered channels ([easyChannel] / [mediumChannel]).
     */
    suspend fun getSolvableDeal(difficulty: DealDifficulty = DealDifficulty.EASY): BoardState {
        return when (difficulty) {
            DealDifficulty.RANDOM -> dealProvider()
            DealDifficulty.EASY, DealDifficulty.MEDIUM -> {
                if (seedBank != null) {
                    val seed = seedBank.consumeSeed(difficulty)
                    if (seed != null) {
                        checkAndReplenish(difficulty)
                        return KlondikeDealer.dealFromSeed(seed)
                    }
                }
                when (difficulty) {
                    DealDifficulty.EASY -> {
                        if (easyJob?.isActive != true && scope.isActive) {
                            startEasyProducer()
                        }
                        easyChannel.receive()
                    }
                    DealDifficulty.MEDIUM -> {
                        if (mediumJob?.isActive != true && scope.isActive) {
                            startMediumProducer()
                        }
                        mediumChannel.receive()
                    }
                    DealDifficulty.RANDOM -> dealProvider()
                }
            }
        }
    }

    /**
     * Returns true if any background generator coroutine is currently active.
     */
    val isRunning: Boolean
        get() = easyJob?.isActive == true || mediumJob?.isActive == true || activeWorkers.get() > 0
}
