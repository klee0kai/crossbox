package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.processor.poet.genGetter
import com.github.klee0kai.crossbox.processor.poet.genProperty
import com.github.klee0kai.crossbox.processor.poet.genSetter
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

fun TypeSpec.Builder.genProxyProperty(
    originName: String,
    property: KSPropertyDeclaration
) = genProperty(
    property.simpleName.asString(),
    property.type.resolve().toClassName(),
) {
    addModifiers(KModifier.OVERRIDE)

    genGetter {
        addStatement("return ${originName}.%L", property.simpleName.asString())
    }

    if (property.isMutable) {
        mutable(property.isMutable)
        genSetter {
            addParameter("newValue", property.type.resolve().toClassName())
            addStatement("${originName}.%L = newValue", property.simpleName.asString())
        }
    }
}


fun FunSpec.Builder.declareSameParameters(
    function: KSFunctionDeclaration,
) = apply {
    function.returnType?.resolve()?.toClassName()?.let { returns(it) }
    function.extensionReceiver?.resolve()?.toClassName()?.let { receiver(it) }

    function.parameters.forEach { param ->
        addParameter(
            ParameterSpec.builder(
                name = param.name?.asString() ?: "",
                type = param.type.resolve().toTypeName(),
            ).apply {
                if (param.isVararg) addModifiers(KModifier.VARARG)
            }.build()
        )
    }
}