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

@CrossboxModel(merge = false)
@CrossboxRsqlFilter
data class DeepRsqlModel(
    val commonId: Long? = null,
    val children: List<SimpleModel>? = null,
    val recursiveChildren: List<DeepRsqlModel> = emptyList(),
    val tags: List<String>? = null,
) {
    companion object;
}