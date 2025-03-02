package com.github.klee0kai.crossbox.core


@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxProxyClass(
    val genProperties: Boolean = true,
    val genFunctions: Boolean = true,
)
