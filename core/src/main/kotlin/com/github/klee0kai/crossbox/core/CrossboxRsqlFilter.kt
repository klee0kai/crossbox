package com.github.klee0kai.crossbox.core

/**
 * Generates functions for filtering models by RSQL expressions.
 *
 * RSQL (REST Query Language) is a language for describing text-based filters.
 * It allows users to filter data through REST APIs using string expressions.
 *
 * Examples of RSQL expressions:
 * - `name=="John"`
 * - `age>25`
 * - `email=~"*@example.com"`
 * - `name=="John";age>25` (AND)
 * - `name=="John",name=="Jane"` (OR)
 *
 * ## Usage Examples
 *
 * ### Basic filtering
 * ```kotlin
 * @CrossboxRsqlFilter
 * data class SimpleModel(
 *     val id: Long? = null,
 *     val name: String? = null,
 *     val age: Int? = null,
 * ) {
 *     companion object
 * }
 *
 * val data = listOf(
 *     SimpleModel(1, "Alice", 30),
 *     SimpleModel(2, "Bob", 25),
 *     SimpleModel(3, "Charlie", 35),
 * )
 *
 * // Generated filter function:
 * val filtered = data.filter(rsqlExpression = "age>25")
 * // Result: Alice (30) and Charlie (35)
 * ```
 *
 * ### With nested fields
 * ```kotlin
 * data class Address(
 *     val city: String? = null,
 *     val country: String? = null,
 * ) {
 *     companion object
 * }
 *
 * @CrossboxRsqlFilter
 * data class PersonWithAddress(
 *     val name: String? = null,
 *     val address: Address? = null,
 * ) {
 *     companion object
 * }
 *
 * val people = listOf(...)
 *
 * // Filter by nested fields:
 * val newyorkers = people.filter(rsqlExpression = "address.city==\"New York\"")
 * ```
 *
 * ### With recursive structures (merge = false)
 * ```kotlin
 * @CrossboxRsqlFilter
 * data class DeepRsqlModel(
 *     val commonId: Long? = null,
 *     val name: String? = null,
 *     val children: List<SimpleModel>? = null,
 *     val recursiveChildren: List<DeepRsqlModel> = emptyList(),
 * ) {
 *     companion object
 * }
 *
 * // Can also filter by recursive fields
 * val filtered = data.filter(rsqlExpression = "name==\"Parent\"")
 * ```
 *
 * ### Complex expressions
 * ```kotlin
 * val complex = people.filter(
 *     rsqlExpression = \"name==\\\"Alice\\\";age>25,name==\\\"Bob\\\"\"
 * )
 * // (name == "Alice" AND age > 25) OR (name == "Bob")
 * ```
 *
 * ## Usage Notes
 *
 * **Nested field filtering**
 *    - Filtering by nested fields using dot notation: `field.subfield`
 *
 * **Supported operators**
 *    - `==` - equality
 *    - `!=` - inequality
 *    - `<`, `<=`, `>`, `>=` - comparison (for numeric types)
 *    - `=~` - regular expression
 *    - `;` - logical AND
 *    - `,` - logical OR
 *
 * **Data types**
 *    - String - exact match or regular expression
 *    - Int, Long, Double - numeric comparisons
 *    - Boolean - true/false comparison
 *    - Nullable fields - null values are automatically handled
 *
 * **List fields**
 *    - List fields are treated as collections
 *    - Filtering works on list elements
 *    - Can filter by size or content
 *
 * **Performance**
 *    - RSQL filtering uses introspection to access fields
 *    - For large datasets, it's recommended to filter at the database level
 *    - This is an in-memory filtering utility
 *
 * **Companion object is mandatory**
 *    ```kotlin
 *    companion object  // Don't forget!
 *    ```
 *
 * **String escaping**
 *    - Strings in RSQL expressions must be enclosed in double quotes
 *    - Special characters require escaping
 *    - When building strings dynamically, use URI encoding for security
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxRsqlFilter(
    val commonRegistry: Boolean = false,
)
