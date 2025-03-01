package com.github.klee0kai.crossbox.processor.poet

import com.squareup.kotlinpoet.*

@DslMarker
annotation class FileSpecDsl

@FileSpecDsl
fun genFileSpec(
    packageName: String,
    fileName: String,
    block: FileSpec.Builder.() -> Unit,
): FileSpec {
    return FileSpec.builder(packageName, fileName)
        .also(block)
        .build()
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
fun FileSpec.Builder.genClass(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
) {
    addType(
        TypeSpec.classBuilder(className)
            .apply(block)
            .build()
    )
}


@FileSpecDsl
fun FileSpec.Builder.genObject(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
) {
    addType(
        TypeSpec.objectBuilder(className)
            .apply(block)
            .build()
    )
}

@FileSpecDsl
fun FileSpec.Builder.genInterface(
    className: ClassName,
    block: TypeSpec.Builder.() -> Unit = {},
) {
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


