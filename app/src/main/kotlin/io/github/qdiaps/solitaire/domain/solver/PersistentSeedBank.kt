package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Persisted snapshot of active available seeds and played history.
 */
@Serializable
data class SeedBankState(
    val easySeeds: List<Long> = emptyList(),
    val mediumSeeds: List<Long> = emptyList(),
    val playedSeeds: Set<Long> = emptySet()
)

/**
 * Storage interface abstraction for reading/writing [SeedBankState].
 */
interface SeedBankStorage {
    suspend fun load(): SeedBankState?
    suspend fun save(state: SeedBankState)
}

/**
 * Lightweight in-memory [SeedBankStorage] implementation for testing and ephemeral sessions.
 */
class InMemorySeedBankStorage(
    initialState: SeedBankState? = null
) : SeedBankStorage {
    private var currentState: SeedBankState? = initialState

    override suspend fun load(): SeedBankState? = currentState

    override suspend fun save(state: SeedBankState) {
        currentState = state
    }
}

/**
 * Thread-safe rotating seed bank.
 *
 * Manages available pre-verified solvable seeds, consumption without replacement,
 * non-repeating played history, and asynchronous persistence.
 */
class PersistentSeedBank(
    private val storage: SeedBankStorage,
    private val defaultCatalogProvider: () -> SeedBankCatalog = { SeedBankCatalog() },
    private val scope: CoroutineScope? = null
) {
    companion object {
        const val TARGET_CAPACITY: Int = 100
        const val REPLENISH_THRESHOLD: Int = 90
        const val DEEP_DEPLETION_THRESHOLD: Int = 50
        const val MAX_PLAYED_HISTORY_SIZE: Int = 10_000
    }

    private val _state = MutableStateFlow(SeedBankState())
    val state: StateFlow<SeedBankState> = _state.asStateFlow()

    @Volatile
    var isInitialized: Boolean = false
        private set

    private val lock = Any()

    suspend fun initialize() {
        val loaded = storage.load()
        if (loaded != null && (loaded.easySeeds.isNotEmpty() || loaded.mediumSeeds.isNotEmpty() || loaded.playedSeeds.isNotEmpty())) {
            _state.value = loaded
        } else {
            val defaultCatalog = defaultCatalogProvider()
            val initialState = SeedBankState(
                easySeeds = defaultCatalog.easySeeds,
                mediumSeeds = defaultCatalog.mediumSeeds,
                playedSeeds = emptySet()
            )
            _state.value = initialState
            persist(initialState)
        }
        isInitialized = true
    }

    fun getAvailableCount(difficulty: DealDifficulty): Int = synchronized(lock) {
        when (difficulty) {
            DealDifficulty.EASY -> _state.value.easySeeds.size
            DealDifficulty.MEDIUM -> _state.value.mediumSeeds.size
            DealDifficulty.RANDOM -> 0
        }
    }

    fun consumeSeed(difficulty: DealDifficulty): Long? = synchronized(lock) {
        val current = _state.value
        val pool = when (difficulty) {
            DealDifficulty.EASY -> current.easySeeds
            DealDifficulty.MEDIUM -> current.mediumSeeds
            DealDifficulty.RANDOM -> return null
        }

        if (pool.isEmpty()) return null

        val randomIndex = Random.nextInt(pool.size)
        val selectedSeed = pool[randomIndex]

        val updatedPool = pool.toMutableList().apply { removeAt(randomIndex) }
        val updatedPlayed = current.playedSeeds.toMutableSet().apply {
            if (size >= MAX_PLAYED_HISTORY_SIZE) {
                val oldest = first()
                remove(oldest)
            }
            add(selectedSeed)
        }

        val newState = when (difficulty) {
            DealDifficulty.EASY -> current.copy(easySeeds = updatedPool, playedSeeds = updatedPlayed)
            DealDifficulty.MEDIUM -> current.copy(mediumSeeds = updatedPool, playedSeeds = updatedPlayed)
            DealDifficulty.RANDOM -> current
        }

        _state.value = newState
        persist(newState)
        return selectedSeed
    }

    fun addSeed(difficulty: DealDifficulty, seed: Long): Boolean = synchronized(lock) {
        val current = _state.value
        if (seed in current.playedSeeds) return false

        when (difficulty) {
            DealDifficulty.EASY -> {
                if (seed in current.easySeeds) return false
                val newState = current.copy(easySeeds = current.easySeeds + seed)
                _state.value = newState
                persist(newState)
                true
            }
            DealDifficulty.MEDIUM -> {
                if (seed in current.mediumSeeds) return false
                val newState = current.copy(mediumSeeds = current.mediumSeeds + seed)
                _state.value = newState
                persist(newState)
                true
            }
            DealDifficulty.RANDOM -> false
        }
    }

    fun flush(percentage: Float = 0.9f): Int = synchronized(lock) {
        val current = _state.value
        val easyKeep = ((1f - percentage) * current.easySeeds.size).toInt().coerceAtLeast(0)
        val mediumKeep = ((1f - percentage) * current.mediumSeeds.size).toInt().coerceAtLeast(0)

        val newEasy = current.easySeeds.take(easyKeep)
        val newMedium = current.mediumSeeds.take(mediumKeep)

        val removed = (current.easySeeds.size - newEasy.size) + (current.mediumSeeds.size - newMedium.size)
        val newState = current.copy(easySeeds = newEasy, mediumSeeds = newMedium)
        _state.value = newState
        persist(newState)
        return removed
    }

    private fun persist(state: SeedBankState) {
        if (scope != null) {
            scope.launch {
                storage.save(state)
            }
        }
    }
}
