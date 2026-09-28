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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background deal generator maintaining buffers of pre-verified solvable Klondike deals
 * segmented by [DealDifficulty].
 *
 * Runs generation loops on [dispatcher], checking random deals against [SolvabilityChecker],
 * classifying their human playability via [difficultyClassifier], and routing verified
 * solvable boards into respective buffered channels ([easyChannel] and [mediumChannel]).
 *
 * When a buffer is full, the respective producer coroutine suspends via backpressure
 * until deals are consumed via [getSolvableDeal], ensuring zero wasted CPU cycles or battery drain.
 *
 * For [DealDifficulty.RANDOM], deals are generated immediately via [dealProvider] without
 * solver latency or buffering overhead.
 *
 * @param scope Lifecycle-aware [CoroutineScope] managing background generation jobs.
 * @param dispatcher Dispatcher for CPU-intensive deal generation and solving (default: [Dispatchers.Default]).
 * @param bufferCapacity Number of pre-verified solvable deals kept ready in memory per difficulty (default: 3).
 * @param solverConfig Solver parameters used for checking generated deals.
 * @param dealProvider Provider function generating candidate deals (default: [KlondikeDealer.dealShuffled]).
 * @param solvabilityChecker Checker function verifying deal solvability (default: [SolvabilityChecker.checkSolvability]).
 * @param difficultyClassifier Function classifying solvable deals into [DealDifficulty] (default: [DealDifficultyClassifier.classify]).
 */
class DealGenerator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY,
    val solverConfig: SolverConfig = SolverConfig(),
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
    }

    private val easyChannel = Channel<BoardState>(capacity = bufferCapacity)
    private val mediumChannel = Channel<BoardState>(capacity = bufferCapacity)

    private var easyJob: Job? = null
    private var mediumJob: Job? = null

    init {
        require(bufferCapacity > 0) {
            "bufferCapacity must be greater than zero, got: $bufferCapacity"
        }
        start()
    }

    /**
     * Convenience constructor configuring [DealGenerator] with a specific [DrawMode].
     */
    constructor(
        scope: CoroutineScope,
        drawMode: DrawMode,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY
    ) : this(
        scope = scope,
        dispatcher = dispatcher,
        bufferCapacity = bufferCapacity,
        solverConfig = SolverConfig(drawMode = drawMode)
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
     * Stops all background generator jobs.
     */
    fun stop() {
        easyJob?.cancel()
        easyJob = null
        mediumJob?.cancel()
        mediumJob = null
    }

    /**
     * Retrieves the next pre-verified deal matching the requested [difficulty].
     *
     * - [DealDifficulty.EASY]: retrieves from the Easy buffer (0 ms latency).
     * - [DealDifficulty.MEDIUM]: retrieves from the Medium buffer (0 ms latency).
     * - [DealDifficulty.RANDOM]: bypasses the solver and instantly returns a freshly shuffled deal.
     */
    suspend fun getSolvableDeal(difficulty: DealDifficulty = DealDifficulty.EASY): BoardState {
        return when (difficulty) {
            DealDifficulty.RANDOM -> dealProvider()
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
        }
    }

    /**
     * Returns true if any background generator coroutine is currently active.
     */
    val isRunning: Boolean
        get() = easyJob?.isActive == true || mediumJob?.isActive == true
}
