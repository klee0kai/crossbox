package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.Crossbox

@Crossbox(
    fieldList = true,
    merge = true,
    changes = true,
)
class SimpleModel(
    val name: String = "",
    val number: Number = 0,
    val long: Long = 0L,
    val short: Short = 0,
) {
    companion object;
}