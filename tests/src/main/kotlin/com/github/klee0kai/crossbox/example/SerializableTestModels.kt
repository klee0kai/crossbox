package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxSerializableRegistry
import kotlinx.serialization.Serializable

@Serializable
@CrossboxSerializableRegistry
data class UserModel(
    val id: Long,
    val username: String,
    val email: String,
)

@Serializable
@CrossboxSerializableRegistry
data class ProductModel(
    val id: Long,
    val title: String,
    val price: Double,
    val inStock: Boolean = true,
)

@Serializable
@CrossboxSerializableRegistry
data class OrderModel(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
)

@Serializable
@CrossboxSerializableRegistry
data class CategoryModel(
    val id: Long,
    val name: String,
    val description: String? = null,
)
