package com.github.klee0kai.crossbox.core

/**
 * Marks a class for serializer registration in the generated serializer registry.
 *
 * The annotation is processed by the KSP processor and generates:
 * - A central registry containing pairs of KClass and KSerializer for all marked classes
 * - The registry can be used for dynamic serialization/deserialization
 * - Single registry file containing all marked classes across the project
 *
 * ## Usage Example
 *
 * ```kotlin
 * @Serializable
 * @CrossboxSerializable
 * data class User(
 *     val id: Long,
 *     val name: String,
 *     val email: String,
 * )
 *
 * @Serializable
 * @CrossboxSerializable
 * data class Product(
 *     val id: Long,
 *     val title: String,
 *     val price: Double,
 * )
 *
 * // Generated registry in com.github.klee0kai.crossbox.core.crossbox.SerializerRegistry:
 * // val serializerRegistry: Map<KClass<*>, KSerializer<*>> = mapOf(
 * //     User::class to User.serializer(),
 * //     Product::class to Product.serializer(),
 * // )
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Requires kotlinx.serialization @Serializable**
 *    - The class MUST be marked with @Serializable from kotlinx.serialization
 *    - This annotation should be used alongside @Serializable
 *    - Without @Serializable, the generated code will not compile
 *
 * 2. **Companion object not required**
 *    - Unlike @CrossboxModel, this annotation doesn't require a companion object
 *
 * 3. **Generated registry location**
 *    - The registry is generated at: `com.github.klee0kai.crossbox.core.crossbox.SerializerRegistry`
 *    - File name: "SerializerRegistry.kt"
 *    - Contains a public property: `serializerRegistry: Map<KClass<*>, KSerializer<*>>`
 *
 * 4. **Global registry**
 *    - All classes marked with @CrossboxSerializable across the entire project
 *      are collected into a single registry
 *    - The registry is generated only once after all classes are processed
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxSerializableRegistry
