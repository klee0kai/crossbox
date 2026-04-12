package com.github.klee0kai.crossbox.tests

import joinery.DataFrame

/**
 * Extension functions to provide a familiar API for Joinery DataFrame
 * Maps the test API to the actual Joinery DataFrame methods
 */

fun <V> DataFrame<V>.nrow(): Int = length()

fun <V> DataFrame<V>.ncol(): Int = size()

val <V> DataFrame<V>.columns: List<String>
    get() = this.columns().map { it.toString() }

fun <V> DataFrame<V>.rows(): List<List<V>> = (0 until length()).map { row(it) }

fun <V> DataFrame<V>.select(vararg columnNames: String): DataFrame<V> = retain(*columnNames)

fun <V> DataFrame<V>.filter(predicate: (row: Map<String, V>) -> Boolean): DataFrame<V> {
    val cols = columns().map { it.toString() }
    val result = DataFrame<V>(cols)

    for (i in 0 until length()) {
        val rowMap = mutableMapOf<String, V>()
        cols.forEach { col ->
            @Suppress("UNCHECKED_CAST")
            rowMap[col] = get(i, col) as V
        }

        if (predicate(rowMap)) {
            val rowValues = cols.map { get(i, it) as V }
            result.append(rowValues)
        }
    }

    return result
}

// Extension property to access rows as a list of maps
val <V> DataFrame<V>.rows: List<Map<String, V?>>
    get() {
        val cols = columns().map { it.toString() }
        return (0 until length()).map { rowIdx ->
            cols.associate { col ->
                @Suppress("UNCHECKED_CAST")
                col to (get(rowIdx, col) as V?)
            }
        }
    }
