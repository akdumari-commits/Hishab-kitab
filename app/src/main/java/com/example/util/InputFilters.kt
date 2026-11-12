package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * SingleKeyFilter safeguards against duplicate typing and synthetic event loops
 * commonly experienced in streaming emulators and touch translation layers.
 *
 * It filters out:
 * 1. Double/multiple character insertion in a single IME batch (e.g. "" -> "11" or "50" -> "5022")
 * 2. Rapid duplicate events within [cooldownMs] (default 320ms) appending or inserting the exact same character
 */
class SingleKeyFilter(private val cooldownMs: Long = 120L) {
    private var lastInsertTime = 0L
    private var lastChar: Char? = null

    fun filter(oldVal: String, newVal: String): String {
        // If length decreased (backspace / delete) or unchanged, always allow
        if (newVal.length <= oldVal.length) {
            lastChar = null
            return newVal
        }

        val addedLen = newVal.length - oldVal.length
        val now = System.currentTimeMillis()

        // Find the index where new character(s) were inserted
        var prefixLen = 0
        while (prefixLen < oldVal.length && prefixLen < newVal.length && oldVal[prefixLen] == newVal[prefixLen]) {
            prefixLen++
        }
        val insertedSlice = newVal.substring(prefixLen, prefixLen + addedLen)

        // Case 1: Multiple identical characters injected in a single batch (e.g. "55", "00", "555")
        if (insertedSlice.length >= 2 && insertedSlice.all { it == insertedSlice[0] }) {
            val singleChar = insertedSlice[0]
            lastChar = singleChar
            lastInsertTime = now
            val deduplicated = newVal.substring(0, prefixLen) + singleChar + newVal.substring(prefixLen + addedLen)
            return deduplicated
        }

        // Case 2: Single character inserted rapidly within cooldownMs matching the previous character
        if (insertedSlice.length == 1) {
            val singleChar = insertedSlice[0]
            if (singleChar == lastChar && (now - lastInsertTime) < cooldownMs) {
                // Drop synthetic duplicate event
                return oldVal
            }
            lastChar = singleChar
            lastInsertTime = now
            return newVal
        }

        lastChar = null
        return newVal
    }
}

@Composable
fun rememberSingleKeyFilter(cooldownMs: Long = 120L): SingleKeyFilter {
    return remember { SingleKeyFilter(cooldownMs) }
}
