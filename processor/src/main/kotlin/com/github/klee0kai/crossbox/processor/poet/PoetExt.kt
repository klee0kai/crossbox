package com.github.klee0kai.crossbox.processor.poet

import com.github.klee0kai.crossbox.processor.Processor
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec


fun FileSpec.Builder.genLibComment() {
    addFileComment("Generated by Crossbox Library\n")
    addFileComment("Project " + Processor.PROJECT_URL + "\n")
    addFileComment("Copyright (c) 2025 Andrey Kuzubov")
}


fun PropertySpec.asParameter(): ParameterSpec = ParameterSpec.builder(name, type).build()

fun PropertySpec.Builder.initFromConstructor(): PropertySpec.Builder = apply { initializer(build().name) }

fun Collection<CodeBlock>.toCodeBlock(): CodeBlock {
    val blocks = this
    return CodeBlock.builder().apply {
        blocks.forEach {
            add(it)
        }
    }.build()
}
