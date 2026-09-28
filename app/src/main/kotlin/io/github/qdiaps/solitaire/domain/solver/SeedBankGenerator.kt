package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.domain.rules.DealDifficulty
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Generator engine for creating pre-verified solvable Klondike [SeedBankCatalog] instances.
 */
object SeedBankGenerator {

    /**
     * Generates a [SeedBankCatalog] containing [targetEasy] Easy seeds and [targetMedium] Medium seeds.
     *
     * Evaluates candidate seeds in parallel batches using deterministic state thresholds
     * and classifies them via [DealDifficultyClassifier.classify].
     *
     * @param targetEasy Number of verified Easy seeds to collect.
     * @param targetMedium Number of verified Medium seeds to collect.
     * @param startSeed Starting seed number.
     * @param drawMode Draw mode applied for validation (default: [DrawMode.DRAW_ONE]).
     * @param batchSize Number of seeds per parallel evaluation batch.
     */
    fun generateCatalog(
        targetEasy: Int = 100,
        targetMedium: Int = 100,
        startSeed: Long = 10_000L,
        drawMode: DrawMode = DrawMode.DRAW_ONE,
        batchSize: Long = 50L
    ): SeedBankCatalog {
        val easySeeds = ConcurrentLinkedQueue<Long>()
        val mediumSeeds = ConcurrentLinkedQueue<Long>()
        val easyCount = AtomicInteger(0)
        val mediumCount = AtomicInteger(0)
        val currentBatchStart = AtomicLong(startSeed)

        val solverConfig = SolverConfig(
            maxStates = 2_000,
            timeoutMs = 1_000L,
            drawMode = drawMode
        )

        while (easyCount.get() < targetEasy || mediumCount.get() < targetMedium) {
            val batchStart = currentBatchStart.getAndAdd(batchSize)
            val batch = (batchStart until (batchStart + batchSize)).toList()

            batch.parallelStream().forEach { seed ->
                if (easyCount.get() >= targetEasy && mediumCount.get() >= targetMedium) {
                    return@forEach
                }

                val board = KlondikeDealer.dealFromSeed(seed)
                val result = SolvabilityChecker.checkSolvability(board, solverConfig)

                if (result is SolvabilityResult.Solvable) {
                    val classification = DealDifficultyClassifier.classify(board, result, drawMode)
                    when (classification) {
                        DealDifficulty.EASY -> {
                            if (easyCount.get() < targetEasy) {
                                if (easyCount.incrementAndGet() <= targetEasy) {
                                    easySeeds.add(seed)
                                }
                            }
                        }
                        DealDifficulty.MEDIUM -> {
                            if (mediumCount.get() < targetMedium) {
                                if (mediumCount.incrementAndGet() <= targetMedium) {
                                    mediumSeeds.add(seed)
                                }
                            }
                        }
                        null, DealDifficulty.RANDOM -> Unit
                    }
                }
            }
        }

        return SeedBankCatalog(
            version = 1,
            easySeeds = easySeeds.toList().take(targetEasy).sorted(),
            mediumSeeds = mediumSeeds.toList().take(targetMedium).sorted()
        )
    }
}

/**
 * Entry point for CLI / Gradle execution of seed pre-generation.
 */
fun main(args: Array<String>) {
    val outputPath = args.getOrNull(0) ?: "src/main/assets/deals/seed_bank.json"
    val file = File(outputPath)
    println("Starting Klondike Seed Bank Pre-Generation...")
    println("Targets: 100 Easy seeds, 100 Medium seeds")
    println("Output: ${file.absolutePath}")

    val startTime = System.currentTimeMillis()
    val catalog = SeedBankGenerator.generateCatalog(targetEasy = 100, targetMedium = 100)
    val duration = System.currentTimeMillis() - startTime

    val json = SeedBankParser.serialize(catalog)
    file.parentFile?.mkdirs()
    file.writeText(json, Charsets.UTF_8)

    println("Successfully generated ${catalog.totalSeedsCount} seeds in ${duration}ms (${catalog.easySeeds.size} Easy, ${catalog.mediumSeeds.size} Medium).")
    println("Wrote catalog to ${file.absolutePath}")
}
