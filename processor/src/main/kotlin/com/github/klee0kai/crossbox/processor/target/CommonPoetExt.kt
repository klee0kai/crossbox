package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.processor.poet.genGetter
import com.github.klee0kai.crossbox.processor.poet.genProperty
import com.github.klee0kai.crossbox.processor.poet.genSetter
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

val KSFunctionDeclaration.isSuspend: Boolean get() = modifiers.contains(Modifier.SUSPEND)

val KSFunctionDeclaration.asyncReturnType: TypeName?
    get() {
        val returnType = returnType?.resolve()?.toClassName()
        return when {
            returnType != null && !isSuspend -> returnType
            returnType != null && returnType != Unit::class.asClassName() ->
                Deferred::class.asClassName().parameterizedBy(returnType)

            isSuspend -> Job::class.asClassName()
            else -> returnType
        }
    }


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