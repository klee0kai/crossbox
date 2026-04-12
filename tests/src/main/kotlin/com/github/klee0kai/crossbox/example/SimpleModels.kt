package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.CrossboxRsqlFilter

@CrossboxModel
@CrossboxRsqlFilter
data class SimpleModel(
    val someIdField: Long? = null,
    val someNameField: String? = null,
    val anyCountField: Number? = null,
    val somePrefixFlagsField: Short? = null,
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

@CrossboxModel
data class DeepRsqlModel(
    val commonId: Long? = null,
    val simpleModel: List<SimpleModel>? = null,
    val tags: List<String>? = null,
) {
    companion object;
}