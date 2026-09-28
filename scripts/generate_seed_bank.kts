#!/usr/bin/env kotlin
/**
 * Klondike Solitaire - Seed Bank Catalog Pre-Generation Script
 *
 * Evaluates candidate seeds using A* solver with aggressive fast-fail bounds
 * and human-playability heuristics, generating 200 guaranteed solvable seeds
 * (100 Easy and 100 Medium).
 *
 * Usage:
 *   kotlin scripts/generate_seed_bank.kts [output_path]
 *
 * Default output: app/src/main/assets/deals/seed_bank.json
 */

import java.io.File

val targetEasy = 100
val targetMedium = 100
val outputPath = args.getOrNull(0) ?: "app/src/main/assets/deals/seed_bank.json"

println("Starting Klondike Seed Bank Pre-Generation...")
println("Targets: $targetEasy Easy seeds, $targetMedium Medium seeds")
println("Output target: $outputPath")

val catalog = io.github.qdiaps.solitaire.domain.solver.SeedBankGenerator.generateCatalog(
    targetEasy = targetEasy,
    targetMedium = targetMedium
)

val json = io.github.qdiaps.solitaire.domain.solver.SeedBankParser.serialize(catalog)
val file = File(outputPath)
file.parentFile?.mkdirs()
file.writeText(json, Charsets.UTF_8)

println("Successfully pre-generated ${catalog.totalSeedsCount} seeds (${catalog.easySeeds.size} Easy, ${catalog.mediumSeeds.size} Medium).")
println("Asset written to: ${file.absolutePath}")
