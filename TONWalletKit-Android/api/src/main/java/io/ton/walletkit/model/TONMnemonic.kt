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
package io.ton.walletkit.model

/**
 * Represents a TON mnemonic (seed phrase) for wallet recovery.
 *
 * This class provides:
 * - Storage and validation of mnemonic words
 * - Support for 12-word (128-bit) and 24-word (256-bit) mnemonics
 * - Mutable word updating for UI input handling
 *
 * @property words The mnemonic words, padded to max length with empty strings
 */
class TONMnemonic private constructor(
    private val words: MutableList<String>,
) {
    /**
     * The current mnemonic words as an immutable list.
     */
    val value: List<String>
        get() = words.toList()

    /**
     * Checks if the mnemonic is fully filled (all required words are non-empty).
     *
     * @return true if the mnemonic has a valid length (12 or 24 non-empty words)
     */
    val isFilled: Boolean
        get() {
            if (words.isEmpty()) return false

            var count = 0
            for (word in words) {
                if (word.isEmpty()) break
                count++
            }

            return TONMnemonicLength.fromWordCount(count) != null
        }

    /**
     * The number of non-empty words in the mnemonic.
     */
    val filledWordCount: Int
        get() = words.indexOfFirst { it.isEmpty() }.let { if (it == -1) words.size else it }

    companion object {
        /**
         * Creates an empty mnemonic with max capacity (24 words).
         */
        fun empty(): TONMnemonic = TONMnemonic(MutableList(TONMnemonicLength.MAX.wordCount) { "" })

        /**
         * Creates a mnemonic from a list of words.
         *
         * @param words The mnemonic words (will be padded or truncated to max length)
         */
        fun fromWords(words: List<String>): TONMnemonic = TONMnemonic(
            (0 until TONMnemonicLength.MAX.wordCount).map { index ->
                words.getOrNull(index) ?: ""
            }.toMutableList(),
        )

        /**
         * Creates a mnemonic from a space-separated string.
         *
         * @param phrase The mnemonic phrase with words separated by spaces
         */
        fun fromPhrase(phrase: String): TONMnemonic = fromWords(phrase.split(" "))
    }

    /**
     * Updates a word at the specified index.
     *
     * @param index The word index (0-based)
     * @param word The new word value
     */
    fun updateWord(index: Int, word: String) {
        if (index in words.indices) {
            words[index] = word
        }
    }

    /**
     * Gets the word at the specified index.
     *
     * @param index The word index (0-based)
     * @return The word at the index, or empty string if out of bounds
     */
    fun getWord(index: Int): String = words.getOrElse(index) { "" }

    /**
     * Converts the mnemonic to a space-separated phrase.
     *
     * @return The mnemonic phrase with all words joined by spaces
     */
    fun toPhrase(): String = words.filter { it.isNotEmpty() }.joinToString(" ")

    override fun toString(): String = toPhrase()
}

/**
 * Supported mnemonic lengths.
 *
 * @property wordCount The number of words in the mnemonic
 * @property bitStrength The entropy strength in bits
 */
enum class TONMnemonicLength(val wordCount: Int, val bitStrength: Int) {
    /**
     * 12-word mnemonic (128-bit entropy)
     */
    BITS_128(12, 128),

    /**
     * 24-word mnemonic (256-bit entropy)
     */
    BITS_256(24, 256),
    ;

    companion object {
        /**
         * The maximum mnemonic length.
         */
        val MAX = BITS_256

        /**
         * Gets the mnemonic length for the given word count.
         *
         * @param count The number of words
         * @return The corresponding TONMnemonicLength, or null if invalid
         */
        fun fromWordCount(count: Int): TONMnemonicLength? =
            entries.find { it.wordCount == count }
    }
}
