package com.github.klee0kai.crossbox.tests.table

import com.github.klee0kai.crossbox.example.Address
import com.github.klee0kai.crossbox.example.PersonComplex
import com.github.klee0kai.crossbox.example.PersonWithAddress
import com.github.klee0kai.crossbox.example.PersonWithTags
import com.github.klee0kai.crossbox.example.crossbox.toTableSaw
import kotlin.test.*


class TableSawNestedTests {

    @Test
    fun nestedModelFlattingTest() {
        val person = PersonWithAddress(
            id = 1L,
            name = "John Doe",
            address = Address(
                street = "123 Main St",
                city = "New York",
                zipCode = "10001"
            )
        )

        val table = person.toTableSaw()

        // Table should have columns for flattened address fields
        assertEquals(1, table.rowCount())

        // Check columns exist with proper flattening naming
        val columnNames = (0 until table.columnCount()).map { table.column(it).name() }
        assertTrue(columnNames.contains("id"), "Should have 'id' column")
        assertTrue(columnNames.contains("name"), "Should have 'name' column")
        assertTrue(columnNames.contains("address_street"), "Should have 'address_street' column")
        assertTrue(columnNames.contains("address_city"), "Should have 'address_city' column")
        assertTrue(columnNames.contains("address_zipCode"), "Should have 'address_zipCode' column")
    }

    @Test
    fun multipleNestedModelsTest() {
        val people = listOf(
            PersonWithAddress(
                id = 1L,
                name = "Alice",
                address = Address(street = "Baker St", city = "London", zipCode = "SW1A")
            ),
            PersonWithAddress(
                id = 2L,
                name = "Bob",
                address = Address(street = "5th Ave", city = "NYC", zipCode = "10001")
            ),
            PersonWithAddress(
                id = 3L,
                name = "Charlie",
                address = Address(street = "Broadway", city = "NYC", zipCode = "10002")
            )
        )

        val table = people.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(5, table.columnCount())
    }

    @Test
    fun nullableNestedModelTest() {
        val people = listOf(
            PersonWithAddress(
                id = 1L,
                name = "Alice",
                address = Address(street = "Baker St", city = "London")
            ),
            PersonWithAddress(
                id = 2L,
                name = "Bob",
                address = null // Nested model is null
            ),
            PersonWithAddress(
                id = 3L,
                name = "Charlie",
                address = Address(street = "Broadway", city = "NYC", zipCode = "10002")
            )
        )

        val table = people.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(5, table.columnCount())

        // Check that missing values are properly handled for null nested model
        assertTrue(table.column(2).isMissing(1), "address_street should be missing at row 1")
        assertTrue(table.column(3).isMissing(1), "address_city should be missing at row 1")
    }

    @Test
    fun listSerializationTest() {
        val person = PersonWithTags(
            id = 1L,
            name = "Alice",
            tags = listOf("java", "kotlin", "python")
        )

        val table = person.toTableSaw()

        assertEquals(1, table.rowCount())
        assertEquals(3, table.columnCount())

        // Tags should be serialized as string representation
        val tagsColumn = table.stringColumn("tags")
        assertNotNull(tagsColumn)
        assertTrue(tagsColumn.get(0).contains("java"), "Tags should contain serialized list")
    }

    @Test
    fun multipleListsTest() {
        val people = listOf(
            PersonWithTags(
                id = 1L,
                name = "Alice",
                tags = listOf("java", "kotlin")
            ),
            PersonWithTags(
                id = 2L,
                name = "Bob",
                tags = listOf("python", "go", "rust")
            ),
            PersonWithTags(
                id = 3L,
                name = "Charlie",
                tags = null
            )
        )

        val table = people.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(3, table.columnCount())

        // Check that null lists are handled properly
        assertTrue(table.stringColumn("tags").isMissing(2), "Tags should be missing for null list")
    }

    @Test
    fun complexModelWithNestedAndListsTest() {
        val person = PersonComplex(
            id = 1L,
            name = "Jane Smith",
            address = Address(street = "789 Oak Ave", city = "San Francisco", zipCode = "94102"),
            phone = "+1-415-555-0199",
            interests = listOf("AI", "Machine Learning", "Data Science"),
            ratings = listOf(4.5, 3.8, 5.0)
        )

        val table = person.toTableSaw()

        assertEquals(1, table.rowCount())

        // Should have: id, name, address_street, address_city, address_zipCode, phone, interests, ratings
        val columnCount = table.columnCount()
        assertEquals(8, columnCount, "Should have 8 columns (id, name, address fields, phone, interests, ratings)")
    }

    @Test
    fun mixedNullableFieldsInComplexModelTest() {
        val people = listOf(
            PersonComplex(
                id = 1L,
                name = "Alice",
                address = Address(street = "Main St", city = "NYC"),
                phone = "111-111-1111",
                interests = listOf("AI"),
                ratings = listOf(5.0)
            ),
            PersonComplex(
                id = 2L,
                name = "Bob",
                address = null,
                phone = null,
                interests = null,
                ratings = null
            ),
            PersonComplex(
                id = 3L,
                name = "Charlie",
                address = Address(street = "Broadway", city = "NYC", zipCode = "10001"),
                phone = "333-333-3333",
                interests = null,
                ratings = null
            )
        )

        val table = people.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(8, table.columnCount())

        // Check missing values
        val addressStreetCol = table.stringColumn("address_street")
        assertFalse(addressStreetCol.isMissing(0), "Row 0 should have address_street")
        assertTrue(addressStreetCol.isMissing(1), "Row 1 should have missing address_street")
        assertFalse(addressStreetCol.isMissing(2), "Row 2 should have address_street")
    }

    @Test
    fun emptyListHandlingTest() {
        val person = PersonWithTags(
            id = 1L,
            name = "Alice",
            tags = emptyList() // Empty list
        )

        val table = person.toTableSaw()

        assertEquals(1, table.rowCount())
        val tagsColumn = table.stringColumn("tags")
        assertNotNull(tagsColumn)
        // Empty list should be serialized as empty collection representation
        assertTrue(tagsColumn.get(0).isNotEmpty(), "Empty list should still be serialized")
    }

    @Test
    fun complexNestedAndListCombinationTest() {
        val people = listOf(
            PersonComplex(
                id = 1L,
                name = "DataEngineer",
                address = Address(street = "Tech Plaza", city = "SF", zipCode = "94102"),
                phone = "555-0101",
                interests = listOf("Big Data", "Scala", "Spark"),
                ratings = listOf(4.8, 4.9, 4.7)
            ),
            PersonComplex(
                id = 2L,
                name = "WebDeveloper",
                address = Address(street = "Dev Road", city = "Austin", zipCode = "78701"),
                phone = "555-0102",
                interests = listOf("React", "Node.js", "TypeScript", "CSS"),
                ratings = listOf(4.5, 4.6, 4.4, 4.3)
            ),
            PersonComplex(
                id = 3L,
                name = "DevOps",
                address = Address(street = "Cloud Tower", city = "Seattle"),
                phone = null,
                interests = listOf("Kubernetes", "Docker", "AWS"),
                ratings = listOf(5.0)
            )
        )

        val table = people.toTableSaw()

        assertEquals(3, table.rowCount())
        assertEquals(8, table.columnCount())

        // Verify some data integrity
        val idColumn = table.longColumn("id")
        assertEquals(1L, idColumn.get(0))
        assertEquals(2L, idColumn.get(1))
        assertEquals(3L, idColumn.get(2))

        // Verify nested fields
        val addressCityColumn = table.stringColumn("address_city")
        assertEquals("SF", addressCityColumn.get(0))
        assertEquals("Austin", addressCityColumn.get(1))
        assertEquals("Seattle", addressCityColumn.get(2))
    }
}
