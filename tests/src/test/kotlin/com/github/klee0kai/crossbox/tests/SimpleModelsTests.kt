package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.changes
import com.github.klee0kai.crossbox.example.crossbox.crossboxFieldList
import com.github.klee0kai.crossbox.example.crossbox.merge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleModelsTests {

    @Test
    fun modelFieldListTest() {
        val list = SimpleModel.crossboxFieldList
        assertEquals(listOf("someIdField", "someNameField", "anyCountField", "somePrefixFlagsField"), list.map { it.name })
    }

    @Test
    fun modelMergeTest() {
        val oneSourceModel = SimpleModel(
            someNameField = "name",
            anyCountField = 1,
        )
        val secondSourceModel = SimpleModel(
            someIdField = 2,
        )

        val merged = oneSourceModel.merge(secondSourceModel)

        assertEquals(
            SimpleModel(
                someNameField = "name",
                anyCountField = 1,
                someIdField = 2,
            ),
            merged
        )
    }

    @Test
    fun modelChangedTest() {
        val oneSourceModel = SimpleModel(someNameField = "name", anyCountField = 1)
        val secondSourceModel = oneSourceModel.copy(anyCountField = 2)

        var nameChangedCount = 0
        var changedCount = 0
        oneSourceModel.changes(
            changed = secondSourceModel,
            someNameFieldChanged = {
                nameChangedCount++
            },
            anyCountFieldChanged = {
                changedCount++
            }
        )

        assertEquals(1, changedCount)
        assertEquals(0, nameChangedCount)
    }


}