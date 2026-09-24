package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.model.BoardState
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
 * Background deal generator maintaining a buffer of pre-verified solvable Klondike deals.
 *
 * Runs a continuous generation loop on [dispatcher], checking random deals against
 * [SolvabilityChecker], and placing verified solvable boards into a buffered [Channel].
 * When the buffer is full, the producer coroutine suspends via backpressure until deals
 * are consumed via [getSolvableDeal], ensuring zero wasted CPU cycles or memory bloat.
 *
 * @param scope Lifecycle-aware [CoroutineScope] managing background generation jobs.
 * @param dispatcher Dispatcher for CPU-intensive deal generation and solving (default: [Dispatchers.Default]).
 * @param bufferCapacity Number of pre-verified solvable deals kept ready in memory (default: 3).
 * @param solverConfig Solver parameters used for checking generated deals.
 * @param dealProvider Provider function generating candidate deals (default: [KlondikeDealer.dealShuffled]).
 * @param solvabilityChecker Checker function verifying deal solvability (default: [SolvabilityChecker.checkSolvability]).
 */
class DealGenerator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY,
    val solverConfig: SolverConfig = SolverConfig(),
    private val dealProvider: () -> BoardState = { KlondikeDealer.dealShuffled() },
    private val solvabilityChecker: (BoardState, SolverConfig) -> SolvabilityResult = { state, config ->
        SolvabilityChecker.checkSolvability(state, config)
    }
) {
    companion object {
        const val DEFAULT_BUFFER_CAPACITY: Int = 3
    }

    private val channel = Channel<BoardState>(capacity = bufferCapacity)
    private var job: Job? = null

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
     * Starts background deal generation if not already active.
     */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(dispatcher) {
            try {
                while (isActive) {
                    val candidate = dealProvider()
                    val result = solvabilityChecker(candidate, solverConfig)
                    if (result is SolvabilityResult.Solvable) {
                        channel.send(candidate)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    /**
     * Stops the background generator job.
     */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Retrieves the next pre-verified solvable deal from the buffer.
     *
     * If the buffer contains ready deals, returns immediately without blocking.
     * If the buffer is temporarily empty, suspends until the background generator
     * completes verification of the next solvable deal.
     */
    suspend fun getSolvableDeal(): BoardState {
        if (job?.isActive != true && scope.isActive) {
            start()
        }
        return channel.receive()
    }

    /**
     * Returns true if the background generator coroutine is currently active.
     */
    val isRunning: Boolean
        get() = job?.isActive == true
}
