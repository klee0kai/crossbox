package com.github.klee0kai.crossbox.core


@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Crossbox(
    val fieldList: Boolean = false,
    val merge: Boolean = false,
    val changes: Boolean = false,
)
