package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.changes
import com.github.klee0kai.crossbox.example.crossbox.fieldList
import com.github.klee0kai.crossbox.example.crossbox.merge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleModelsTests {

    @Test
    fun modelFieldListTest() {
        val list = SimpleModel.fieldList
        assertEquals(listOf("name", "number", "long", "short"), list.map { it.name })
    }

    @Test
    fun modelMergeTest() {
        val oneSourceModel = SimpleModel(
            name = "name",
            number = 1,
        )
        val secondSourceModel = SimpleModel(
            long = 2,
        )

        val merged = oneSourceModel.merge(secondSourceModel)

        assertEquals(
            SimpleModel(
                name = "name",
                number = 1,
                long = 2,
            ),
            merged
        )
    }

    @Test
    fun modelChangedTest() {
        val oneSourceModel = SimpleModel(name = "name", number = 1)
        val secondSourceModel = oneSourceModel.copy(number = 2)

        var nameChangedCount = 0
        var changedCount = 0
        oneSourceModel.changes(
            changed = secondSourceModel,
            nameChanged = {
                nameChangedCount++
            },
            numberChanged = {
                changedCount++
            }
        )

        assertEquals(1, changedCount)
        assertEquals(0, nameChangedCount)
    }


}