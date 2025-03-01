package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxModel

@CrossboxModel(
    fieldList = true,
    merge = true,
    changes = true,
)
class SimpleModel(
    val name: String? = null,
    val number: Number? = null,
    val long: Long? = null,
    val short: Short? = null,
) {
    companion object;
}

@CrossboxModel(
    fieldList = true,
    merge = true,
    changes = true,
)
class DeepMergeSimpleModel(
    val long: Long? = null,
    val simpleModel: SimpleModel? = null,
) {
    companion object;
}