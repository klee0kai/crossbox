package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxSerializable
import kotlinx.serialization.Serializable

@Serializable
@CrossboxSerializable
data class UserModel(
    val id: Long,
    val username: String,
    val email: String,
)

@Serializable
@CrossboxSerializable
data class ProductModel(
    val id: Long,
    val title: String,
    val price: Double,
    val inStock: Boolean = true,
)

@Serializable
@CrossboxSerializable
data class OrderModel(
    val id: Long,
    val userId: Long,
    val productId: Long,
    val quantity: Int,
)

@Serializable
@CrossboxSerializable
data class CategoryModel(
    val id: Long,
    val name: String,
    val description: String? = null,
)
