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
import org.junit.Test
import java.io.File

/**
 * Byte-level snapshot of the generator output.
 *
 * Each `.kt` written by `Scripts/generate-api/generate-test-models.sh` into
 * `api/src/test/java/io/ton/walletkit/api/generatedtest/` is compared against a
 * checked-in reference at `api/src/test/resources/reference-test-models/`.
 * Whitespace is stripped from both before comparison so cosmetic churn from an
 * openapi-generator version bump (extra blank lines, indentation tweaks) does
 * not flake the suite — only structural changes (added imports, reordered
 * fields, renamed properties, removed annotations) cause a failure.
 *
 * This complements the reflection-based `GeneratedModelsTest`: that file pins
 * specific shape invariants per fixture pattern; this one pins the full text.
 *
 * **Workflow when this test fails after a regeneration:**
 *
 *   1. Run `Scripts/generate-api/generate-test-models.sh <kit-path>` — this
 *      overwrites `generatedtest/` with the new generator output.
 *   2. This test fails, listing the drifted files.
 *   3. Diff the two directories to see what changed:
 *        diff -ru \
 *          api/src/test/resources/reference-test-models \
 *          api/src/test/java/io/ton/walletkit/api/generatedtest
 *   4. If the change is intentional (template update, fixture change, accepted
 *      generator upgrade), promote the new output by copying every regenerated
 *      `.kt` from `api/src/test/java/io/ton/walletkit/api/generatedtest/` over
 *      to `api/src/test/resources/reference-test-models/`. Commit both
 *      directories together so reviewers see the diff.
 *   5. If the change is unintentional, fix the fixture, template, or generator
 *      config — do NOT update the reference.
 */
class GeneratedModelsSnapshotTest {

    private val generatedDir = File("src/test/java/io/ton/walletkit/api/generatedtest")
    private val referenceDir = File("src/test/resources/reference-test-models")

    @Test
    fun `generated and reference directories contain the same set of files`() {
        val genFiles = generatedDir.listKotlinFileNames()
        val refFiles = referenceDir.listKotlinFileNames()
        assertEquals(
            "File set under generatedtest/ does not match reference-test-models/. " +
                "Either a new model was generated without updating the reference, or " +
                "a reference was committed without a matching generated file.",
            refFiles,
            genFiles,
        )
    }

    @Test
    fun `each generated file matches its reference after whitespace normalization`() {
        val drifted = mutableListOf<String>()
        val genFiles = generatedDir.kotlinFiles()
        assertTrue(
            "Expected generated test models in $generatedDir; run generate-test-models.sh.",
            genFiles.isNotEmpty(),
        )
        for (genFile in genFiles) {
            val refFile = File(referenceDir, genFile.name)
            if (!refFile.exists()) {
                drifted += "${genFile.name} (no reference file)"
                continue
            }
            if (genFile.readText().normalize() != refFile.readText().normalize()) {
                drifted += genFile.name
            }
        }
        if (drifted.isNotEmpty()) {
            fail(
                "Generated test models drifted from the reference snapshot:\n  - " +
                    drifted.joinToString("\n  - ") +
                    "\n\nReview the diff and, if the change is intentional, promote it by " +
                    "copying generatedtest/ over reference-test-models/. See class KDoc " +
                    "for the full workflow.",
            )
        }
    }

    private fun File.kotlinFiles(): List<File> =
        (listFiles { f -> f.isFile && f.extension == "kt" } ?: emptyArray()).sortedBy { it.name }

    private fun File.listKotlinFileNames(): List<String> = kotlinFiles().map { it.name }

    private fun String.normalize(): String = this.filterNot { it.isWhitespace() }
}
