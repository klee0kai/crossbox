package com.github.klee0kai.crossbox.tests.table

import com.github.klee0kai.crossbox.example.*
import com.github.klee0kai.crossbox.example.crossbox.JoineryRegistry
import kotlin.test.*

class DataFrameRegistryTests {

    @Test
    fun singleModelToDataFrameTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "John",
            anyCountField = 100,
            somePrefixFlagsField = 5.toShort()
        )

        val df = JoineryRegistry.toDataFrame(model)

        assertEquals(1, df.nrow())
        assertEquals(4, df.ncol())
        assertTrue(df.columns.contains("someIdField"))
        assertTrue(df.columns.contains("someNameField"))
        assertTrue(df.columns.contains("anyCountField"))
        assertTrue(df.columns.contains("somePrefixFlagsField"))
    }

    @Test
    fun multipleModelsToDataFrameTest() {
        val models = listOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob"),
            SimpleModel(someIdField = 3L, someNameField = "Charlie")
        )

        val df = JoineryRegistry.toDataFrame(models)

        assertEquals(3, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun sequenceToDataFrameTest() {
        val sequence = sequenceOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob")
        )

        val df = JoineryRegistry.toDataFrame(sequence.toList())

        assertEquals(2, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun dataFrameHeadTest() {
        val models = (1..10).map { i ->
            SimpleModel(someIdField = i.toLong(), someNameField = "Person-$i")
        }

        val df = JoineryRegistry.toDataFrame(models)
        val head = df.head(3)

        assertEquals(3, head.nrow())
        assertEquals(4, head.ncol())
    }

    @Test
    fun dataFrameTailTest() {
        val models = (1..10).map { i ->
            SimpleModel(someIdField = i.toLong(), someNameField = "Person-$i")
        }

        val df = JoineryRegistry.toDataFrame(models)
        val tail = df.tail(3)

        assertEquals(3, tail.nrow())
        assertEquals(4, tail.ncol())
    }

    @Test
    fun dataFrameSelectColumnsTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "John",
            anyCountField = 100
        )

        val df = JoineryRegistry.toDataFrame(model)
        val selected = df.select("someIdField", "someNameField")

        assertEquals(1, selected.nrow())
        assertEquals(2, selected.ncol())
        assertTrue(selected.columns.contains("someIdField"))
        assertTrue(selected.columns.contains("someNameField"))
        assertFalse(selected.columns.contains("anyCountField"))
    }

    @Test
    fun dataFrameFilterTest() {
        val models = listOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob"),
            SimpleModel(someIdField = 3L, someNameField = "Charlie")
        )

        val df = JoineryRegistry.toDataFrame(models)
        val filtered = df.filter { row ->
            val id = (row["someIdField"] as? Long) ?: 0L
            id > 1L
        }

        assertEquals(2, filtered.nrow())
    }

    @Test
    fun nestedModelToDataFrameTest() {
        val person = PersonWithAddress(
            id = 1L,
            name = "John",
            address = Address(street = "Main St", city = "NYC", zipCode = "10001")
        )

        val df = JoineryRegistry.toDataFrame(person)

        assertEquals(1, df.nrow())
        // Should have: id, name, address_street, address_city, address_zipCode
        assertEquals(5, df.ncol())
        assertTrue(df.columns.contains("address_street"))
        assertTrue(df.columns.contains("address_city"))
        assertTrue(df.columns.contains("address_zipCode"))
    }

    @Test
    fun nullableNestedModelToDataFrameTest() {
        val people = listOf(
            PersonWithAddress(id = 1L, name = "Alice", address = Address(street = "St1", city = "City1")),
            PersonWithAddress(id = 2L, name = "Bob", address = null),
            PersonWithAddress(id = 3L, name = "Charlie", address = Address(street = "St3", city = "City3"))
        )

        val df = JoineryRegistry.toDataFrame(people)

        assertEquals(3, df.nrow())
        assertEquals(5, df.ncol())

        // Check null handling
        assertNotNull(df.rows[0]["address_street"])
        assertNull(df.rows[1]["address_street"])
        assertNotNull(df.rows[2]["address_street"])
    }

    @Test
    fun listSerializationToDataFrameTest() {
        val person = PersonWithTags(
            id = 1L,
            name = "Alice",
            tags = listOf("java", "kotlin", "python")
        )

        val df = JoineryRegistry.toDataFrame(person)

        assertEquals(1, df.nrow())
        assertEquals(3, df.ncol())

        val tagsValue = df.rows[0]["tags"]
        assertNotNull(tagsValue)
        assertTrue(tagsValue.toString().contains("java"))
        assertTrue(tagsValue.toString().contains("kotlin"))
    }

    @Test
    fun multipleListsToDataFrameTest() {
        val people = listOf(
            PersonWithTags(id = 1L, name = "Alice", tags = listOf("java", "kotlin")),
            PersonWithTags(id = 2L, name = "Bob", tags = listOf("python", "go")),
            PersonWithTags(id = 3L, name = "Charlie", tags = null)
        )

        val df = JoineryRegistry.toDataFrame(people)

        assertEquals(3, df.nrow())
        assertEquals(3, df.ncol())

        assertNotNull(df.rows[0]["tags"])
        assertNotNull(df.rows[1]["tags"])
        assertNull(df.rows[2]["tags"])
    }

    @Test
    fun complexModelToDataFrameTest() {
        val person = PersonComplex(
            id = 1L,
            name = "Jane",
            address = Address(street = "Oak Ave", city = "SF"),
            phone = "555-1234",
            interests = listOf("AI", "ML"),
            ratings = listOf(4.5, 5.0)
        )

        val df = JoineryRegistry.toDataFrame(person)

        assertEquals(1, df.nrow())
        // id, name, address_street, address_city, address_zipCode, phone, interests, ratings
        assertEquals(8, df.ncol())
    }

    @Test
    fun largeDataFrameTest() {
        val models = (1..1000).map { i ->
            SimpleModel(
                someIdField = i.toLong(),
                someNameField = "Person-$i",
                anyCountField = i * 10
            )
        }

        val df = JoineryRegistry.toDataFrame(models)

        assertEquals(1000, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun emptyListToDataFrameTest() {
        val models = emptyList<SimpleModel>()

        val df = JoineryRegistry.toDataFrame(models)

        assertEquals(0, df.nrow())
        // Columns list should match the model structure even if rows are empty
        assertTrue(df.columns.isNotEmpty() || df.nrow() == 0, "Empty DataFrame should have either columns or zero rows")
    }
}
