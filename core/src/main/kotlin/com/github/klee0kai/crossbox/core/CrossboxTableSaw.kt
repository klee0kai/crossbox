package com.github.klee0kai.crossbox.core

/**
 * Generates extensions for converting models to TableSaw tables.
 *
 * TableSaw is a library for working with tabular data in Java/Kotlin.
 * The annotation generates extension functions:
 * - `Iterable<Model>.toTableSaw(): Table`
 * - `Sequence<Model>.toTableSaw(): Table`
 * - `Model.toTableSaw(): Table` (single row)
 * - `List<Model>.createEmptyTableSaw(): Table` (with empty schema)
 *
 * ## Usage Examples
 *
 * ### Basic list conversion
 * ```kotlin
 * @CrossboxModel
 * @CrossboxTableSaw
 * data class Person(
 *     val id: Long? = null,
 *     val name: String? = null,
 *     val age: Int? = null,
 * ) {
 *     companion object
 * }
 *
 * val people = listOf(
 *     Person(1, "Alice", 30),
 *     Person(2, "Bob", 25),
 * )
 *
 * // Generated function:
 * val table = people.toTableSaw()
 * println(table.print())
 * ```
 *
 * ### With nested model (automatic expansion)
 * ```kotlin
 * @CrossboxModel
 * data class Address(
 *     val street: String? = null,
 *     val city: String? = null,
 * ) {
 *     companion object
 * }
 *
 * @CrossboxModel
 * @CrossboxTableSaw
 * data class PersonWithAddress(
 *     val id: Long? = null,
 *     val name: String? = null,
 *     val address: Address? = null,
 * ) {
 *     companion object
 * }
 *
 * val data = listOf(
 *     PersonWithAddress(1, "Alice", Address("Main St", "NY")),
 * )
 *
 * val table = data.toTableSaw()
 * // Generates columns: id, name, address_street, address_city
 * // Nested fields are expanded with the original field prefix
 * ```
 *
 * ### With List fields (serialized to JSON)
 * ```kotlin
 * @CrossboxModel
 * @CrossboxTableSaw
 * data class PersonWithTags(
 *     val id: Long? = null,
 *     val tags: List<String>? = null,  // Serialized to JSON string
 * ) {
 *     companion object
 * }
 *
 * val data = listOf(PersonWithTags(1, listOf("java", "kotlin")))
 * val table = data.toTableSaw()
 * // Column tags contains: "[java, kotlin]"
 * ```
 *
 * ### Working with Sequence (for large datasets)
 * ```kotlin
 * val sequence = generateSequence(1) { if (it < 1000) it + 1 else null }
 *     .map { Person(it.toLong(), "User$it", 20 + it % 50) }
 *
 * val table = sequence.toTableSaw()
 * // Sequence is converted to List, then to Table
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Requires @CrossboxModel**
 *    - Usually used together with @CrossboxModel
 *    - Ensures compatibility of the generated code
 *
 * 2. **Nested model expansion**
 *    - Nested model must have @CrossboxModel annotation
 *    - Fields are expanded only one level deep
 *    - Column name: `parentField_childField`
 *    - Example: `address.street` → column `address_street`
 *
 * 3. **List types are always serialized**
 *    - `List<T>`, `java.util.List<T>`, `Sequence<T>`
 *    - Converted to string via `.toString()`
 *    - Stored as StringColumn in Table
 *
 * 4. **Supported primitive types**
 *    - String → StringColumn
 *    - Int, Short, Byte → IntColumn (Short and Byte are cast)
 *    - Long → LongColumn
 *    - Double, Float → DoubleColumn
 *    - Boolean → BooleanColumn
 *    - Number → DoubleColumn
 *    - LocalDate → DateColumn
 *    - LocalDateTime → DateTimeColumn
 *    - LocalTime → TimeColumn
 *    - BigDecimal → DoubleColumn
 *    - Other types → StringColumn
 *
 * 5. **Null values**
 *    - For nullable fields, `appendMissing()` is used for null values
 *    - Missing (null) values in TableSaw represent absence of data
 *
 * 6. **Performance**
 *    - The `createEmptyTableSaw()` function creates an empty table with the correct schema
 *    - Useful for initializing tables before adding data
 *
 * 7. **Numeric types specifics**
 *    - java.lang.Number is cast to Double for TableSaw compatibility
 *    - Byte and Short are automatically converted to IntColumn
 *
 * 8. **Companion object is mandatory**
 *    ```kotlin
 *    companion object  // Don't forget!
 *    ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxTableSaw(
    val commonRegistry: Boolean = false,
)
