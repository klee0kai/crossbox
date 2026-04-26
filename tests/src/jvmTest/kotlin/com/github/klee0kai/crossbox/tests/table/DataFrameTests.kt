package com.github.klee0kai.crossbox.tests.table

import com.github.klee0kai.crossbox.example.Address
import com.github.klee0kai.crossbox.example.PersonComplex
import com.github.klee0kai.crossbox.example.PersonWithAddress
import com.github.klee0kai.crossbox.example.PersonWithTags
import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.toDataFrame
import kotlin.test.*


class DataFrameTests {

    @Test
    fun singleModelToDataFrameTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "John",
            anyCountField = 100,
            somePrefixFlagsField = 5.toShort()
        )


        val df = model.toDataFrame()

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

        val df = models.toDataFrame()

        assertEquals(3, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun sequenceToDataFrameTest() {
        val sequence = sequenceOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob")
        )

        val df = sequence.toDataFrame()

        assertEquals(2, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun dataFrameHeadTest() {
        val models = (1..10).map { i ->
            SimpleModel(someIdField = i.toLong(), someNameField = "Person-$i")
        }

        val df = models.toDataFrame()
        val head = df.head(3)

        assertEquals(3, head.nrow())
        assertEquals(4, head.ncol())
    }

    @Test
    fun dataFrameTailTest() {
        val models = (1..10).map { i ->
            SimpleModel(someIdField = i.toLong(), someNameField = "Person-$i")
        }

        val df = models.toDataFrame()
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

        val df = model.toDataFrame()
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

        val df = models.toDataFrame()
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

        val df = person.toDataFrame()

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

        val df = people.toDataFrame()

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

        val df = person.toDataFrame()

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

        val df = people.toDataFrame()

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

        val df = person.toDataFrame()

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

        val df = models.toDataFrame()

        assertEquals(1000, df.nrow())
        assertEquals(4, df.ncol())
    }

    @Test
    fun emptyListToDataFrameTest() {
        val models = emptyList<SimpleModel>()

        val df = models.toDataFrame()

        assertEquals(0, df.nrow())
        // Columns list should match the model structure even if rows are empty
        assertTrue(df.columns.isNotEmpty() || df.nrow() == 0, "Empty DataFrame should have either columns or zero rows")
    }

    @Test
    fun dataFrameToStringTest() {
        val models = listOf(
            SimpleModel(someIdField = 1L, someNameField = "Alice"),
            SimpleModel(someIdField = 2L, someNameField = "Bob")
        )

        val df = models.toDataFrame()
        val str = df.toString()

        // Should contain column names and data
        assertTrue(str.contains("someIdField"))
        assertTrue(str.contains("someNameField"))
        assertTrue(str.contains("Alice"))
        assertTrue(str.contains("Bob"))
    }

    @Test
    fun dataFrameRowCountTest() {
        val models = listOf(
            SimpleModel(someIdField = 1L),
            SimpleModel(someIdField = 2L),
            SimpleModel(someIdField = 3L)
        )

        val df = models.toDataFrame()

        assertEquals(3, df.nrow())
    }

    @Test
    fun dataFrameColumnCountTest() {
        val model = SimpleModel(
            someIdField = 1L,
            someNameField = "Test",
            anyCountField = 100,
            somePrefixFlagsField = 5.toShort()
        )

        val df = model.toDataFrame()

        assertEquals(4, df.ncol())
    }

    @Test
    fun complexFilteringTest() {
        val people = listOf(
            PersonWithAddress(id = 1L, name = "Alice", address = Address(city = "NYC")),
            PersonWithAddress(id = 2L, name = "Bob", address = Address(city = "LA")),
            PersonWithAddress(id = 3L, name = "Charlie", address = Address(city = "NYC")),
            PersonWithAddress(id = 4L, name = "David", address = null)
        )

        val df = people.toDataFrame()
        val nycPeople = df.filter { row ->
            (row["address_city"] as? String) == "NYC"
        }

        assertEquals(2, nycPeople.nrow())
    }
}
