package com.github.klee0kai.crossbox.tests.table

import com.github.klee0kai.crossbox.example.Address
import com.github.klee0kai.crossbox.example.PersonWithAddress
import com.github.klee0kai.crossbox.example.crossbox.TableSawRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TableSawRegistryTests {

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

        val table = TableSawRegistry.toTableSaw(person)

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

        val table = TableSawRegistry.toTableSaw(people)

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

        val table = TableSawRegistry.toTableSaw(people)

        assertEquals(3, table.rowCount())
        assertEquals(5, table.columnCount())

        // Check that missing values are properly handled for null nested model
        assertTrue(table.column(2).isMissing(1), "address_street should be missing at row 1")
        assertTrue(table.column(3).isMissing(1), "address_city should be missing at row 1")
    }

}
