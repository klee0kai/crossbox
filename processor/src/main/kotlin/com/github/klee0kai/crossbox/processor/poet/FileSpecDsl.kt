package com.github.klee0kai.crossbox.processor.poet

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

@DslMarker
annotation class FileSpecDsl

@FileSpecDsl
fun CodeGenerator.genKtFile(
    dependencies: Dependencies,
    packageName: String,
    fileName: String,
    block: FileSpec.Builder.() -> Unit,
) {
    FileSpec.builder(packageName, fileName)
        .also(block)
        .build()
        .writeTo(
            codeGenerator = this,
            dependencies = dependencies,
        )
}

@FileSpecDsl
fun FileSpec.Builder.genProperty(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit = {}
) {
    addProperty(
        PropertySpec.builder(name, type, *modifiers)
            .apply(block)
            .build()
    )
}

@FileSpecDsl
fun FileSpec.Builder.genClass(className: ClassName, block: TypeSpec.Builder.() -> Unit = {}) {
    addType(
        TypeSpec.classBuilder(className)
            .apply(block)
            .build()
    )
}


@FileSpecDsl
fun FileSpec.Builder.genObject(className: ClassName, block: TypeSpec.Builder.() -> Unit = {}) {
    addType(
        TypeSpec.objectBuilder(className)
            .apply(block)
            .build()
    )
}

@FileSpecDsl
fun FileSpec.Builder.genInterface(className: ClassName, block: TypeSpec.Builder.() -> Unit = {}) {
    addType(
        TypeSpec.interfaceBuilder(className)
            .apply(block)
            .build()
    )
}

@FileSpecDsl
fun FileSpec.Builder.genFun(
    name: String,
    block: FunSpec.Builder.() -> Unit = {},
) {
    addFunction(
        FunSpec.builder(name)
            .apply(block)
            .build()
    )
}


