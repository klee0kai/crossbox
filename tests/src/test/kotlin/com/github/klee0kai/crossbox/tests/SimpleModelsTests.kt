package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.fieldList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleModelsTests {

    @Test
    fun testFieldList() {
        val list = SimpleModel.fieldList
        assertEquals(listOf("name", "number", "long", "short"), list.map { it.name })
    }

}