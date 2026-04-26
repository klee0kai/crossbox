package com.github.klee0kai.crossbox.tests.table

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.createEmptyTableSaw
import com.github.klee0kai.crossbox.example.crossbox.toTableSaw
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TableSawTests {

    @Test
    fun singleModelToTableSawTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "John",
            anyCountField = 100,
            somePrefixFlagsField = 5.toShort()
        )

        val table = model.toTableSaw()

        assertEquals(1, table.rowCount())
        assertEquals(4, table.columnCount())
        assertEquals("someIdField", table.column(0).name())
        assertEquals("someNameField", table.column(1).name())
        assertEquals("anyCountField", table.column(2).name())
        assertEquals("somePrefixFlagsField", table.column(3).name())
    }

    @Test
    fun iterableModelToTableSawTest() {
        val models = listOf(
            SimpleModel(
                someIdField = 1L,
                someNameField = "John",
                anyCountField = 100,
                somePrefixFlagsField = 5.toShort()
            ),
            SimpleModel(
                someIdField = 2L,
                someNameField = "Jane",
                anyCountField = 200,
                somePrefixFlagsField = 10.toShort()
            ),
            SimpleModel(
                someIdField = 3L,
                someNameField = "Bob",
                anyCountField = 150,
                somePrefixFlagsField = 7.toShort()
            )
        )

        val table = models.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(4, table.columnCount())
        assertEquals(listOf("someIdField", "someNameField", "anyCountField", "somePrefixFlagsField"),
            (0 until table.columnCount()).map { table.column(it).name() })
    }

    @Test
    fun sequenceModelToTableSawTest() {
        val sequence = sequenceOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob")
        )

        val table = sequence.toTableSaw()

        assertEquals(2, table.rowCount())
        assertEquals(4, table.columnCount())
    }

    @Test
    fun emptyListToTableSawTest() {
        val models = emptyList<SimpleModel>()

        val table = models.toTableSaw()

        assertEquals(0, table.rowCount())
        assertEquals(4, table.columnCount())
    }

    @Test
    fun nullableFieldsHandlingTest() {
        val models = listOf(
            SimpleModel(
                someIdField = 1L,
                someNameField = "John",
                anyCountField = 100,
                somePrefixFlagsField = 5.toShort()
            ),
            SimpleModel(
                someIdField = 2L,
                someNameField = null, // null value
                anyCountField = null, // null value
                somePrefixFlagsField = null // null value
            ),
            SimpleModel(
                someIdField = null, // null value
                someNameField = "Charlie",
                anyCountField = 300,
                somePrefixFlagsField = 15.toShort()
            )
        )

        val table = models.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(4, table.columnCount())

        // Check that nulls are properly handled as missing values
        assertTrue(table.column(1).isMissing(1)) // someNameField at row 1 is null
        assertTrue(table.column(2).isMissing(1)) // anyCountField at row 1 is null
        assertTrue(table.column(3).isMissing(1)) // somePrefixFlagsField at row 1 is null
        assertTrue(table.column(0).isMissing(2)) // someIdField at row 2 is null
    }

    @Test
    fun createEmptyTableSawTest() {
        val models = emptyList<SimpleModel>()

        val table = models.createEmptyTableSaw()

        assertEquals(0, table.rowCount())
        assertEquals(4, table.columnCount())
        assertEquals("someIdField", table.column(0).name())
        assertEquals("someNameField", table.column(1).name())
        assertEquals("anyCountField", table.column(2).name())
        assertEquals("somePrefixFlagsField", table.column(3).name())
    }

    @Test
    fun tableColumnsTypesTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "Test",
            anyCountField = 99.5,
            somePrefixFlagsField = 8.toShort()
        )

        val table = model.toTableSaw()

        // Verify column types
        assertEquals("LONG", table.column(0).type().name()) // someIdField: Long
        assertEquals("STRING", table.column(1).type().name()) // someNameField: String
        assertEquals("DOUBLE", table.column(2).type().name()) // anyCountField: Number (Double)
        assertEquals("INTEGER", table.column(3).type().name()) // somePrefixFlagsField: Short (Int)
    }

    @Test
    fun largeDatasetTest() {
        val models = (1..1000).map { i ->
            SimpleModel(
                someIdField = i.toLong(),
                someNameField = "Name-$i",
                anyCountField = i * 10,
                somePrefixFlagsField = (i % 100).toShort()
            )
        }

        val table = models.toTableSaw()

        assertEquals(1000, table.rowCount())
        assertEquals(4, table.columnCount())
    }

    @Test
    fun tableSelectionAfterConversionTest() {
        val models = listOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice", anyCountField = 100),
            SimpleModel(someIdField = 2L, someNameField = "Bob", anyCountField = 200),
            SimpleModel(someIdField = 3L, someNameField = "Charlie", anyCountField = 300)
        )

        val table = models.toTableSaw()

        // Test that we can work with the generated table
        val selectedTable = table.selectColumns("someNameField", "anyCountField")
        assertEquals(2, selectedTable.columnCount())
        assertEquals(3, selectedTable.rowCount())
    }

    @Test
    fun mixedNullableAndNonNullableFieldsTest() {
        val models = listOf(
            SimpleModel(
                someIdField = 1L, // non-null
                someNameField = "John", // non-null
                anyCountField = null, // nullable
                somePrefixFlagsField = 5.toShort() // non-null
            ),
            SimpleModel(
                someIdField = null, // nullable
                someNameField = null, // nullable
                anyCountField = 200, // non-null (if provided)
                somePrefixFlagsField = null // nullable
            )
        )

        val table = models.toTableSaw()

        assertEquals(2, table.rowCount())
        assertEquals(4, table.columnCount())

        // Verify missing values in correct places
        assertFalse(table.column(0).isMissing(0)) // someIdField row 0 is not missing
        assertTrue(table.column(0).isMissing(1)) // someIdField row 1 is missing
        assertTrue(table.column(2).isMissing(0)) // anyCountField row 0 is missing
        assertFalse(table.column(2).isMissing(1)) // anyCountField row 1 is not missing
    }

}
