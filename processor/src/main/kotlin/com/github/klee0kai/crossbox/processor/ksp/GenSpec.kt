package com.github.klee0kai.crossbox.processor.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.FileSpec

data class GenSpec(
    val fileSpec: FileSpec,
    val dependencies: Dependencies,
)
