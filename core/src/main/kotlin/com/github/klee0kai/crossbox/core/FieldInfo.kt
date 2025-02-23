package com.github.klee0kai.crossbox.core

import kotlin.reflect.KClass

data class FieldInfo(
    val name: String,
    val kclass: KClass<*>,
)
