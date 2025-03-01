package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxNotSuspendInterface
import com.github.klee0kai.crossbox.core.CrossboxProxyClass
import com.github.klee0kai.crossbox.core.CrossboxSuspendInterface

@CrossboxGenInterface
@CrossboxSuspendInterface
@CrossboxNotSuspendInterface
@CrossboxProxyClass
open class RegularClass {

    open val greeting: String = "Hello, World!"

    fun sayHello() {
        println(greeting)
    }

    suspend fun sayGoodbye() {
        println("Goodbye")
    }

}