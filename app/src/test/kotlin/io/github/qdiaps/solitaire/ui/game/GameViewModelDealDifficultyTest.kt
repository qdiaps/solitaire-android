package io.github.qdiaps.solitaire.ui.game

import io.github.qdiaps.solitaire.data.model.GameSettings
import io.github.qdiaps.solitaire.data.repository.SettingsRepository
import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.solver.DealGenerator
import io.github.qdiaps.solitaire.domain.solver.SolvabilityResult
import io.github.qdiaps.solitaire.ui.theme.CardBackStyle
import io.github.qdiaps.solitaire.ui.theme.CardFaceStyle
import io.github.qdiaps.solitaire.ui.theme.FeltTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelDealDifficultyTest {




    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTestBoard(id: String): BoardState {
        val card = Card(Suit.SPADES, Rank.ACE, isFaceUp = true, id = id)
        return BoardState(stock = listOf(card))
    }

    private class FakeSettingsRepository(
        initialSettings: GameSettings = GameSettings()
    ) : SettingsRepository {
        private val _flow = MutableStateFlow(initialSettings)
        override val settingsFlow: StateFlow<GameSettings> = _flow.asStateFlow()

        override suspend fun getSettings(): GameSettings = _flow.value
        override suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
            _flow.update(transform)
        }
        override suspend fun setDrawMode(drawMode: DrawMode) { _flow.update { it.copy(drawMode = drawMode) } }
        override suspend fun setDealDifficulty(dealDifficulty: DealDifficulty) { _flow.update { it.copy(dealDifficulty = dealDifficulty) } }
        override suspend fun setLeftHanded(isLeftHanded: Boolean) { _flow.update { it.copy(isLeftHanded = isLeftHanded) } }
        override suspend fun setFeltTheme(feltTheme: FeltTheme) { _flow.update { it.copy(feltTheme = feltTheme) } }
        override suspend fun setCardBackStyle(cardBackStyle: CardBackStyle) { _flow.update { it.copy(cardBackStyle = cardBackStyle) } }
        override suspend fun setCardFaceStyle(cardFaceStyle: CardFaceStyle) { _flow.update { it.copy(cardFaceStyle = cardFaceStyle) } }
        override suspend fun setSoundEnabled(enabled: Boolean) { _flow.update { it.copy(soundEnabled = enabled) } }
        override suspend fun setHapticsEnabled(enabled: Boolean) { _flow.update { it.copy(hapticsEnabled = enabled) } }
        override suspend fun setAutoHintEnabled(enabled: Boolean) { _flow.update { it.copy(autoHintEnabled = enabled) } }
        override suspend fun resetToDefaults() { _flow.value = GameSettings() }
    }

    @Nested
    @DisplayName("Difficulty Integration on New Game")
    inner class NewGameDifficultyTests {

        @Test
        @DisplayName("startNewGame fetches EASY deal when difficulty is EASY")
        fun `startNewGame fetches EASY deal when difficulty is EASY`() = runTest(testDispatcher) {
            val settingsRepo = FakeSettingsRepository()
            val easyBoard = createTestBoard("easy_deal")
            val mediumBoard = createTestBoard("medium_deal")
            val randomBoard = createTestBoard("random_deal")

            var counter = 0
            val generator = DealGenerator(
                scope = backgroundScope,
                dispatcher = testDispatcher,
                bufferCapacity = 2,
                dealProvider = { if (counter++ % 2 == 0) easyBoard else mediumBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(emptyList(), listOf(board), 1, 1L)
                },
                difficultyClassifier = { board, _ ->
                    if (board == mediumBoard) DealDifficulty.MEDIUM else DealDifficulty.EASY
                }
            )

            val viewModel = GameViewModel(
                initialBoardState = createTestBoard("init"),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                dealGenerator = generator,
                dealProvider = { randomBoard },
                settingsRepository = settingsRepo
            )
            testScheduler.runCurrent()

            viewModel.setDealDifficulty(DealDifficulty.EASY)
            testScheduler.runCurrent()

            viewModel.startNewGame()
            testScheduler.runCurrent()

            assertEquals(easyBoard, viewModel.uiState.value.boardState)
            viewModel.stopTimer()
            generator.stop()
        }

        @Test
        @DisplayName("startNewGame fetches MEDIUM deal when difficulty is MEDIUM")
        fun `startNewGame fetches MEDIUM deal when difficulty is MEDIUM`() = runTest(testDispatcher) {
            val settingsRepo = FakeSettingsRepository()
            val easyBoard = createTestBoard("easy_deal")
            val mediumBoard = createTestBoard("medium_deal")
            val randomBoard = createTestBoard("random_deal")

            var counter = 0
            val generator = DealGenerator(
                scope = backgroundScope,
                dispatcher = testDispatcher,
                bufferCapacity = 2,
                dealProvider = { if (counter++ % 2 == 0) easyBoard else mediumBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(emptyList(), listOf(board), 500, 1L)
                },
                difficultyClassifier = { board, _ ->
                    if (board == mediumBoard) DealDifficulty.MEDIUM else DealDifficulty.EASY
                }
            )

            val viewModel = GameViewModel(
                initialBoardState = createTestBoard("init"),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                dealGenerator = generator,
                dealProvider = { randomBoard },
                settingsRepository = settingsRepo
            )
            testScheduler.runCurrent()

            viewModel.setDealDifficulty(DealDifficulty.MEDIUM)
            testScheduler.runCurrent()

            viewModel.startNewGame()
            testScheduler.runCurrent()

            assertEquals(mediumBoard, viewModel.uiState.value.boardState)
            viewModel.stopTimer()
            generator.stop()
        }

        @Test
        @DisplayName("startNewGame bypasses DealGenerator when difficulty is RANDOM")
        fun `startNewGame bypasses DealGenerator when difficulty is RANDOM`() = runTest(testDispatcher) {
            val settingsRepo = FakeSettingsRepository()
            val randomBoard = createTestBoard("random_deal")

            var generatorCalled = false
            val generator = DealGenerator(
                scope = backgroundScope,
                dispatcher = testDispatcher,
                bufferCapacity = 2,
                dealProvider = {
                    generatorCalled = true
                    createTestBoard("generated")
                },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(emptyList(), listOf(board), 1, 1L)
                }
            )

            val viewModel = GameViewModel(
                initialBoardState = createTestBoard("init"),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                dealGenerator = generator,
                dealProvider = { randomBoard },
                settingsRepository = settingsRepo
            )
            testScheduler.runCurrent()

            viewModel.setDealDifficulty(DealDifficulty.RANDOM)
            testScheduler.runCurrent()

            generatorCalled = false
            viewModel.startNewGame()
            testScheduler.runCurrent()

            assertEquals(randomBoard, viewModel.uiState.value.boardState)
            assertFalse(generatorCalled)
            viewModel.stopTimer()
            generator.stop()
        }
    }

    @Nested
    @DisplayName("SetDealDifficulty Intent and Persistence")
    inner class IntentAndPersistenceTests {

        @Test
        @DisplayName("SetDealDifficulty intent updates UI state and settings repository")
        fun `SetDealDifficulty intent updates state and repository`() = runTest(testDispatcher) {
            val settingsRepo = FakeSettingsRepository()
            val viewModel = GameViewModel(
                initialBoardState = createTestBoard("init"),
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                settingsRepository = settingsRepo
            )
            testScheduler.runCurrent()

            assertEquals(DealDifficulty.EASY, viewModel.uiState.value.dealDifficulty)

            viewModel.onIntent(GameIntent.SetDealDifficulty(DealDifficulty.MEDIUM))
            testScheduler.runCurrent()

            assertEquals(DealDifficulty.MEDIUM, viewModel.uiState.value.dealDifficulty)
            assertEquals(DealDifficulty.MEDIUM, settingsRepo.getSettings().dealDifficulty)
            viewModel.stopTimer()
        }
    }

    @Nested
    @DisplayName("Cold Start Async Initialization with DealGenerator")
    inner class ColdStartAsyncInitTests {

        @Test
        @DisplayName("Cold start without saved session applies solvable deal from DealGenerator")
        fun `cold start applies solvable deal from DealGenerator`() = runTest(testDispatcher) {
            val settingsRepo = FakeSettingsRepository()
            val solvableBoard = createTestBoard("solvable_cold_start")
            val rawBoard = createTestBoard("raw_fallback")

            val generator = DealGenerator(
                scope = backgroundScope,
                dispatcher = testDispatcher,
                bufferCapacity = 2,
                dealProvider = { solvableBoard },
                solvabilityChecker = { board, _ ->
                    SolvabilityResult.Solvable(emptyList(), listOf(board), 1, 1L)
                },
                difficultyClassifier = { _, _ -> DealDifficulty.EASY }
            )

            val viewModel = GameViewModel(
                coroutineScope = backgroundScope,
                timerDispatcher = testDispatcher,
                autoStartTimer = false,
                dealGenerator = generator,
                dealProvider = { rawBoard },
                settingsRepository = settingsRepo
            )

            testScheduler.runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(solvableBoard, viewModel.uiState.value.boardState)
            viewModel.stopTimer()
            generator.stop()
        }
    }
}
