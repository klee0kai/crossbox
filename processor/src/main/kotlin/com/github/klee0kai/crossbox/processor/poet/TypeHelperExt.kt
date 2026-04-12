package com.github.klee0kai.crossbox.processor.poet

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName

fun TypeName.rawType(): TypeName {
    val typeName = this
    if (typeName is ParameterizedTypeName) {
        if (!typeName.typeArguments.isEmpty()) {
            return typeName.typeArguments.first().rawType()
        }
    }
    if (typeName is WildcardTypeName) {
        if (!typeName.outTypes.isEmpty()) return typeName.outTypes.first().rawType()
    }
    return typeName.copy(nullable = false)
}