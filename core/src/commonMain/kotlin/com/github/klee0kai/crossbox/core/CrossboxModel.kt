package com.github.klee0kai.crossbox.core

/**
 * Generates utility and helper functions for working with data models.
 *
 * The annotation is processed by the KSP processor and generates:
 * - Functions to work with the model's field list
 * - Functions for deep merging of models (if `merge = true`)
 * - Functions for tracking field changes (if `changes = true`)
 *
 * ## Usage Examples
 *
 * ### Basic usage
 * ```kotlin
 * @CrossboxModel
 * data class User(
 *     val id: Long? = null,
 *     val name: String? = null,
 *     val email: String? = null,
 * ) {
 *     companion object
 * }
 *
 * // Generated function to get all fields:
 * // User.fieldList()
 * ```
 *
 * ### With disabled merge (for deep hierarchies)
 * ```kotlin
 * @CrossboxModel(merge = false)
 * data class DeepRsqlModel(
 *     val commonId: Long? = null,
 *     val children: List<SimpleModel>? = null,
 *     val recursiveChildren: List<DeepRsqlModel> = emptyList(),
 * ) {
 *     companion object
 * }
 * // merge = false saves resources for deeply nested models
 * ```
 *
 * ### With multiple annotations combined
 * ```kotlin
 * @CrossboxModel
 * @CrossboxTableSaw
 * @CrossboxRsqlFilter
 * data class SimpleModel(
 *     val id: Long? = null,
 *     val name: String? = null,
 * ) {
 *     companion object
 * }
 * // Generates functions for TableSaw, RSQL filters and base utilities
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Mandatory companion object** - the class must have an empty companion object:
 *    ```kotlin
 *    companion object  // This is required!
 *    ```
 *    Static functions will be generated there.
 *
 * 2. **fieldList parameter (default: true)**
 *    - When true, generates a function to get the list of all fields
 *    - Used for introspection and working with dynamic structures
 *
 * 3. **merge parameter (default: true)**
 *    - When true, generates functions for merging two models (deep merge)
 *    - Useful for updating existing objects with partial data
 *    - Recommended to disable for recursive data structures
 *
 * 4. **changes parameter (default: true)**
 *    - When true, generates functions for tracking changes
 *    - Allows comparing two objects and getting only changed fields
 *
 * 5. **Nullable and Default values**
 *    - All fields should be nullable or have default values
 *    - This ensures correct behavior during merge operations
 *
 * 6. **Nested models**
 *    - If a nested model is also marked with @CrossboxModel, it is integrated
 *
 * 7. **List and Collection types**
 *    - During merge operations, List fields are replaced entirely, not merged
 *
 * @param fieldList Generate functions for working with the field list
 * @param merge Generate functions for merging models
 * @param changes Generate functions for tracking changes
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxModel(
    val fieldList: Boolean = true,
    val merge: Boolean = true,
    val changes: Boolean = true,
)
