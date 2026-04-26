package com.github.klee0kai.crossbox.tests.rsql

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.RsqlFilterRegistry
import com.github.klee0kai.crossbox.example.crossbox.matchesRsqlQuery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RSQL Registry Tests")
class RsqlFilterRegistryTests {


    @Test
    @DisplayName("Complex real-world query")
    fun testComplexQuery() {
        val models = listOf(
            SimpleModel(
                someNameField = "Alice",
                anyCountField = 100,
                someIdField = 1000L,
                somePrefixFlagsField = 10
            ),
            SimpleModel(someNameField = "Bob", anyCountField = 200, someIdField = 2000L, somePrefixFlagsField = 20),
            SimpleModel(
                someNameField = "Charlie",
                anyCountField = 150,
                someIdField = 1500L,
                somePrefixFlagsField = 15
            ),
            SimpleModel(
                someNameField = "Diana",
                anyCountField = 300,
                someIdField = 3000L,
                somePrefixFlagsField = 30
            ),
        )

        // Find: (someIdField >= 1500) AND (someNameField != "Alice") AND (somePrefixFlagsField in [15,20,30])
        val result = RsqlFilterRegistry.filterByRsqlQuery(
            models,
            "someIdField=gte=1500;someNameField!=Alice;somePrefixFlagsField=in=15,20,30"
        )

        Assertions.assertEquals(3, result.size)
        Assertions.assertTrue(result.all { it.someIdField!! >= 1500 })
        Assertions.assertTrue(result.all { it.someNameField != "Alice" })
        Assertions.assertTrue(result.all { it.somePrefixFlagsField!! in listOf<Short>(15, 20, 30) })
    }

    @Test
    @DisplayName("Filter then match single item")
    fun testFilterThenMatch() {
        val models = listOf(
            SimpleModel(someNameField = "Alice", someIdField = 1000L),
            SimpleModel(someNameField = "Bob", someIdField = 2000L),
        )

        val filtered = RsqlFilterRegistry.filterByRsqlQuery(models, "someIdField=gt=1500")
        Assertions.assertEquals(1, filtered.size)

        val item = filtered[0]
        Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Bob"))
    }

    @Test
    @DisplayName("Builder pattern usage")
    fun testBuilderPatternUsage() {
        val models = listOf(
            SimpleModel(someNameField = "Alice", someIdField = 1000L),
            SimpleModel(someNameField = "Bob", someIdField = 2000L),
            SimpleModel(someNameField = "Charlie", someIdField = 1500L),
        )

        val query = "someIdField=gte=1000;someIdField=lte=2000"
        val inRange = RsqlFilterRegistry.filterByRsqlQuery(models, query)

        Assertions.assertEquals(3, inRange.size)
        inRange.forEach { model ->
            Assertions.assertTrue(model.matchesRsqlQuery(query))
        }
    }

}