package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxModel

@CrossboxModel
data class SimpleModel(
    val name: String? = null,
    val number: Number? = null,
    val long: Long? = null,
    val short: Short? = null,
) {
    companion object;
}

@CrossboxModel
data class DeepMergeSimpleModel(
    val long: Long? = null,
    val simpleModel: SimpleModel? = null,
) {
    companion object;
}