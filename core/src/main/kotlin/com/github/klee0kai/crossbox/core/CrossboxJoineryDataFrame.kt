package com.github.klee0kai.crossbox.core

/**
 * Generates extensions for converting models to Joinery DataFrame.
 *
 * Joinery is a library for working with tabular data (similar to pandas in Python).
 * DataFrame is a table where each row is an object and each column is a property.
 *
 * The annotation generates extension functions:
 * - `Iterable<Model>.toDataFrame(): DataFrame`
 * - `Sequence<Model>.toDataFrame(): DataFrame`
 * - `Model.toDataFrame(): DataFrame` (single row)
 *
 * ## Usage Examples
 *
 * ### Basic list conversion
 * ```kotlin
 * @CrossboxJoineryDataFrame
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
 * val df = people.toDataFrame()
 * println(df.toString())
 * ```
 *
 * ### With nested model
 * ```kotlin
 * data class Address(
 *     val street: String? = null,
 *     val city: String? = null,
 * ) {
 *     companion object
 * }
 *
 * @CrossboxJoineryDataFrame
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
 *     PersonWithAddress(2, "Bob", Address("Oak Ave", "LA")),
 * )
 *
 * val df = data.toDataFrame()
 * // Nested fields are expanded similar to TableSaw
 * ```
 *
 * ### With List and Collections
 * ```kotlin
 * @CrossboxJoineryDataFrame
 * data class PersonWithTags(
 *     val id: Long? = null,
 *     val name: String? = null,
 *     val tags: List<String>? = null,
 * ) {
 *     companion object
 * }
 *
 * val data = listOf(PersonWithTags(1, "Alice", listOf("java", "kotlin")))
 * val df = data.toDataFrame()
 * ```
 *
 * ### Working with Sequence
 * ```kotlin
 * val sequence = (1..100).asSequence()
 *     .map { Person(it.toLong(), "User$it", 20 + it % 50) }
 *
 * val df = sequence.toDataFrame()
 * // Sequence is more optimal than List for large datasets
 * ```
 *
 * ### Converting a single object
 * ```kotlin
 * val person = Person(1, "Alice", 30)
 * val df = person.toDataFrame()  // DataFrame with one row
 * ```
 *
 * ## Usage Notes
 *
 * **Similar to @CrossboxTableSaw**
 *    - Functionality is very close to TableSaw
 *    - Main difference is the library used (Joinery vs TableSaw)
 *    - Choice between them depends on project requirements
 *
 * **Nested model expansion**
 *    - Fields are expanded with prefix: `address_street`, `address_city`
 *    - Only one level of depth is automatically expanded
 *
 * **List and Collection types**
 *    - Serialized to string representation (JSON-like format)
 *    - Stored as text columns in DataFrame
 *    - Can be parsed back if necessary
 *
 * **Supported data types**
 *    - Primitives: String, Int, Long, Double, Float, Boolean
 *    - Temporal: LocalDate, LocalDateTime, LocalTime
 *    - Numeric: BigDecimal, Short, Byte, java.lang.Number
 *    - Collections: List<T>, Set<T>, Sequence<T>
 *
 * **Null values**
 *    - Null values are handled correctly
 *    - DataFrame preserves information about missing values
 *
 * **Performance**
 *    - DataFrame in memory, suitable for medium datasets
 *    - For very large volumes, use DataFrame in database
 *    - Aggregation and filtering operations are performed in memory
 *
 * **Operations with DataFrame**
 *    - After creating DataFrame, you can perform:
 *      - Row filtering
 *      - Column selection
 *      - Data aggregation
 *      - Sorting
 *      - Grouping
 *
 * **Companion object is mandatory**
 *    ```kotlin
 *    companion object  // Don't forget!
 *    ```
 *
 * **Usage with other annotations**
 *     ```kotlin
 *     @CrossboxJoineryDataFrame
 *     data class Data(
 *         val id: Long? = null,
 *         val value: String? = null,
 *     ) {
 *         companion object
 *     }
 *     ```
 *     All annotations work independently and generate different functions.
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxJoineryDataFrame
