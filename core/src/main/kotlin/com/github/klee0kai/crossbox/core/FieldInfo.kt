package com.github.klee0kai.crossbox.core

import kotlin.reflect.KClass

data class FieldInfo<T>(
    val name: String,
    val kclass: KClass<*>,
    val annotations: List<Any> = emptyList(),
    val getValue: T.() -> Any? = { null },
    val setValue: T.(Any) -> Unit = { },
)
