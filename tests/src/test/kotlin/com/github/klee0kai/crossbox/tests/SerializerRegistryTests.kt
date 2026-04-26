@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.example.CategoryModel
import com.github.klee0kai.crossbox.example.OrderModel
import com.github.klee0kai.crossbox.example.ProductModel
import com.github.klee0kai.crossbox.example.UserModel
import com.github.klee0kai.crossbox.example.serializerRegistry
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for CrossboxSerializableProcessor and the generated serializer registry
 */
class SerializerRegistryTests {

    @Test
    fun registryIsGenerated() {
        assertNotNull(serializerRegistry)
        assertTrue(serializerRegistry.isNotEmpty(), "Registry should not be empty")
    }

    @Test
    fun registryContainsAllMarkedClasses() {
        val expectedClasses = setOf(
            UserModel::class,
            ProductModel::class,
            OrderModel::class,
            CategoryModel::class,
        )

        val registryClasses = serializerRegistry.keys

        expectedClasses.forEach { expectedClass ->
            assertTrue(
                expectedClass in registryClasses,
                "Registry should contain ${expectedClass.simpleName}"
            )
        }
    }

    @Test
    fun registryHasCorrectSize() {
        assertEquals(4, serializerRegistry.size, "Registry should contain exactly 4 classes")
    }

    @Test
    fun canSerializeUserModel() {
        val user = UserModel(
            id = 1L,
            username = "john_doe",
            email = "john@example.com"
        )

        @Suppress("UNCHECKED_CAST")
        val json = Json.encodeToString(serializerRegistry[UserModel::class]!! as kotlinx.serialization.KSerializer<UserModel>, user)
        assertNotNull(json)
        assertTrue(json.contains("john_doe"), "JSON should contain username")
        assertTrue(json.contains("john@example.com"), "JSON should contain email")
    }

    @Test
    fun canSerializeProductModel() {
        val product = ProductModel(
            id = 100L,
            title = "Laptop",
            price = 999.99,
            inStock = true
        )

        @Suppress("UNCHECKED_CAST")
        val json = Json.encodeToString(serializerRegistry[ProductModel::class]!! as kotlinx.serialization.KSerializer<ProductModel>, product)
        assertNotNull(json)
        assertTrue(json.contains("Laptop"), "JSON should contain product title")
        assertTrue(json.contains("999.99"), "JSON should contain price")
    }

    @Test
    fun canSerializeOrderModel() {
        val order = OrderModel(
            id = 1000L,
            userId = 1L,
            productId = 100L,
            quantity = 5
        )

        @Suppress("UNCHECKED_CAST")
        val json = Json.encodeToString(serializerRegistry[OrderModel::class]!! as kotlinx.serialization.KSerializer<OrderModel>, order)
        assertNotNull(json)
        assertTrue(json.contains("1000"), "JSON should contain order id")
        assertTrue(json.contains("5"), "JSON should contain quantity")
    }

    @Test
    fun canSerializeCategoryModel() {
        val category = CategoryModel(
            id = 10L,
            name = "Electronics",
            description = "Electronic devices and accessories"
        )

        @Suppress("UNCHECKED_CAST")
        val json = Json.encodeToString(serializerRegistry[CategoryModel::class]!! as kotlinx.serialization.KSerializer<CategoryModel>, category)
        assertNotNull(json)
        assertTrue(json.contains("Electronics"), "JSON should contain category name")
    }

    @Test
    fun canDeserializeUserModel() {
        val userJson = """{"id":1,"username":"jane_doe","email":"jane@example.com"}"""

        @Suppress("UNCHECKED_CAST")
        val user = Json.decodeFromString(serializerRegistry[UserModel::class]!! as kotlinx.serialization.KSerializer<UserModel>, userJson)
        assertEquals(1L, user.id)
        assertEquals("jane_doe", user.username)
        assertEquals("jane@example.com", user.email)
    }

    @Test
    fun canDeserializeProductModel() {
        val productJson = """{"id":200,"title":"Mouse","price":29.99,"inStock":true}"""

        @Suppress("UNCHECKED_CAST")
        val product = Json.decodeFromString(serializerRegistry[ProductModel::class]!! as kotlinx.serialization.KSerializer<ProductModel>, productJson)
        assertEquals(200L, product.id)
        assertEquals("Mouse", product.title)
        assertEquals(29.99, product.price)
        assertEquals(true, product.inStock)
    }

    @Test
    fun serializationRoundTrip() {
        val originalOrder = OrderModel(
            id = 2000L,
            userId = 42L,
            productId = 555L,
            quantity = 10
        )

        @Suppress("UNCHECKED_CAST")
        val serializer = serializerRegistry[OrderModel::class]!! as kotlinx.serialization.KSerializer<OrderModel>
        val json = Json.encodeToString(serializer, originalOrder)
        val deserializedOrder = Json.decodeFromString(serializer, json)

        assertEquals(originalOrder.id, deserializedOrder.id)
        assertEquals(originalOrder.userId, deserializedOrder.userId)
        assertEquals(originalOrder.productId, deserializedOrder.productId)
        assertEquals(originalOrder.quantity, deserializedOrder.quantity)
    }

    @Test
    fun allSerializersAreAccessible() {
        serializerRegistry.forEach { (kClass, serializer) ->
            assertNotNull(kClass, "KClass should not be null")
            assertNotNull(serializer, "Serializer should not be null")
            assertTrue(
                kClass.simpleName in listOf("UserModel", "ProductModel", "OrderModel", "CategoryModel"),
                "Unexpected class in registry: ${kClass.simpleName}"
            )
        }
    }

    @Test
    fun registryKeysMatchValueSerializers() {
        serializerRegistry.forEach { (kClass, serializer) ->
            // Verify that the serializer's serial name matches the class name
            val serialName = serializer.descriptor.serialName
            assertTrue(
                serialName.contains(kClass.simpleName ?: ""),
                "Serializer serial name should contain class name"
            )
        }
    }

    @Test
    fun multipleSerializationsProduceSameResult() {
        val category1 = CategoryModel(id = 1, name = "Books")
        val category2 = CategoryModel(id = 1, name = "Books")

        @Suppress("UNCHECKED_CAST")
        val serializer = serializerRegistry[CategoryModel::class]!! as kotlinx.serialization.KSerializer<CategoryModel>
        val json1 = Json.encodeToString(serializer, category1)
        val json2 = Json.encodeToString(serializer, category2)

        assertEquals(json1, json2, "Same objects should produce identical JSON")
    }

    @Test
    fun partialDeserializationWithDefaults() {
        val productJsonWithoutInStock = """{"id":300,"title":"Keyboard","price":79.99}"""

        @Suppress("UNCHECKED_CAST")
        val product = Json.decodeFromString(serializerRegistry[ProductModel::class]!! as kotlinx.serialization.KSerializer<ProductModel>, productJsonWithoutInStock)
        assertEquals(300L, product.id)
        assertEquals("Keyboard", product.title)
        assertEquals(79.99, product.price)
        assertEquals(true, product.inStock, "Should use default value for inStock")
    }

    @Test
    fun registryIsConsistent() {
        // Call multiple times to ensure consistency
        val registry1Keys = serializerRegistry.keys.map { it.simpleName ?: "" }.sorted()
        val registry2Keys = serializerRegistry.keys.map { it.simpleName ?: "" }.sorted()

        assertEquals(registry1Keys, registry2Keys, "Registry should be consistent across multiple accesses")
    }
}
