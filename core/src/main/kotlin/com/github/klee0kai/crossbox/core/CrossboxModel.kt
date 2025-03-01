package com.github.klee0kai.crossbox.core


@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxModel(
    val fieldList: Boolean = true,
    val merge: Boolean = true,
    val changes: Boolean = true,
)
