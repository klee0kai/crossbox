package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxModel
import com.github.klee0kai.crossbox.core.CrossboxRsqlFilter
import com.github.klee0kai.crossbox.core.CrossboxTableSaw

@CrossboxModel
@CrossboxRsqlFilter
@CrossboxTableSaw
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

// Models for TableSaw nested and list support
@CrossboxModel
data class Address(
    val street: String? = null,
    val city: String? = null,
    val zipCode: String? = null,
) {
    companion object;
}

@CrossboxModel
@CrossboxTableSaw
data class PersonWithAddress(
    val id: Long? = null,
    val name: String? = null,
    val address: Address? = null,
) {
    companion object;
}

@CrossboxModel
@CrossboxTableSaw
data class PersonWithTags(
    val id: Long? = null,
    val name: String? = null,
    val tags: List<String>? = null,
) {
    companion object;
}

@CrossboxModel
@CrossboxTableSaw
data class PersonComplex(
    val id: Long? = null,
    val name: String? = null,
    val address: Address? = null,
    val phone: String? = null,
    val interests: List<String>? = null,
    val ratings: List<Double>? = null,
) {
    companion object;
}