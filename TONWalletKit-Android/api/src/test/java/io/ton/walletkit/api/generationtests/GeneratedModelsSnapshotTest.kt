/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.api.generationtests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Drift detector for the generator pipeline.
 *
 * Runs `Scripts/generate-api/generate-test-models.sh` against the walletkit fixture
 * at test time, into a build-only directory, then compares the freshly produced
 * `.kt` files to the committed snapshot under
 * `api/src/test/java/io/ton/walletkit/api/generatedtest/`. Whitespace is stripped
 * from both sides before the equality check so cosmetic churn from an
 * openapi-generator version bump (extra blank lines, indentation tweaks) does
 * not flake the suite — only structural changes (added imports, reordered
 * fields, renamed properties, removed annotations) cause a failure.
 *
 * This complements the reflection-based `GeneratedModelsTest`: that file pins
 * specific shape invariants per fixture pattern; this one pins the full text.
 *
 * **Opt-in via `WALLETKIT_PATH`.** Running the pipeline needs `pnpm` +
 * `openapi-generator` + a clone of the kit monorepo, so we don't force every
 * dev box to install them. Set `WALLETKIT_PATH` (env var or
 * `-Dwalletkit.path=…` system property) to the absolute path of
 * `<kit-checkout>/packages/walletkit`. When unset, the test is skipped via
 * JUnit's `Assume`.
 *
 * **When this test fails after a regeneration:**
 *
 *   1. Inspect the diff between fresh and committed: `diff -ru` the build dir
 *      printed in the failure message against the committed `generatedtest/`.
 *   2. If the change is intentional (template update, fixture change, accepted
 *      generator upgrade), refresh the snapshot:
 *        Scripts/generate-api/generate-test-models.sh <kit-walletkit-path>
 *      Commit the updated `generatedtest/`.
 *   3. If the change is unintentional, fix the fixture, template, or generator
 *      config — do NOT regenerate.
 */
class GeneratedModelsSnapshotTest {

    private val committedDir = File("src/test/java/io/ton/walletkit/api/generatedtest")
    private val freshDir = File("build/generated-test-models-fresh")
    private val scriptPath = File("../../Scripts/generate-api/generate-test-models.sh")

    @Test
    fun `freshly generated models match the committed snapshot`() {
        val walletkitPath = resolveWalletkitPath()
        assumeTrue(
            "Set WALLETKIT_PATH (env) or -Dwalletkit.path=<kit>/packages/walletkit to run; " +
                "skipped because the snapshot test needs the kit monorepo, pnpm, and openapi-generator.",
            walletkitPath != null,
        )
        requireNotNull(walletkitPath)

        assertTrue(
            "Generator script not found at ${scriptPath.absolutePath} — repo layout shifted?",
            scriptPath.exists(),
        )

        if (freshDir.exists()) freshDir.deleteRecursively()
        freshDir.mkdirs()

        runGenerator(walletkitPath, freshDir.absoluteFile)

        val freshFiles = freshDir.kotlinFiles().associateBy { it.name }
        val committedFiles = committedDir.kotlinFiles().associateBy { it.name }
        assertEquals(
            "File set under build/generated-test-models-fresh/ does not match " +
                "the committed generatedtest/ snapshot. " +
                "A model was added or removed without regenerating the snapshot.",
            committedFiles.keys.sorted(),
            freshFiles.keys.sorted(),
        )

        val drifted = mutableListOf<String>()
        for ((name, fresh) in freshFiles) {
            val committed = committedFiles.getValue(name)
            if (fresh.readText().normalize() != committed.readText().normalize()) {
                drifted += name
            }
        }
        if (drifted.isNotEmpty()) {
            fail(
                "Generator output drifted from the committed snapshot:\n  - " +
                    drifted.joinToString("\n  - ") +
                    "\n\nFresh: ${freshDir.absolutePath}" +
                    "\nCommitted: ${committedDir.absolutePath}" +
                    "\n\nIf the change is intentional, regenerate by running\n" +
                    "  ${scriptPath.absoluteFile.normalize()} $walletkitPath\n" +
                    "and commit the updated generatedtest/. Otherwise fix the " +
                    "fixture, template, or generator config.",
            )
        }
    }

    private fun resolveWalletkitPath(): String? =
        System.getProperty("walletkit.path")?.takeIf { it.isNotBlank() }
            ?: System.getenv("WALLETKIT_PATH")?.takeIf { it.isNotBlank() }

    private fun runGenerator(walletkitPath: String, destDir: File) {
        val process = ProcessBuilder(
            "bash",
            scriptPath.absolutePath,
            walletkitPath,
            destDir.absolutePath,
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        assertEquals(
            "Generator script exited with $exit. Output:\n$output",
            0,
            exit,
        )
    }

    private fun File.kotlinFiles(): List<File> =
        (listFiles { f -> f.isFile && f.extension == "kt" } ?: emptyArray()).sortedBy { it.name }

    private fun String.normalize(): String = this.filterNot { it.isWhitespace() }
}
