package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.core.Crossbox

@Crossbox(
    fieldList = true,
    merge = true,
    changes = true,
)
class SimpleModel(
    val name: String,
    val number: Number,
    val long: Long,
    val short: Short,
)