package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxAsyncInterface
import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxProxyClass
import com.github.klee0kai.crossbox.core.CrossboxSuspendInterface
import com.github.klee0kai.crossbox.example.crossbox.IGreetingClass

@CrossboxGenInterface
@CrossboxSuspendInterface
@CrossboxAsyncInterface
@CrossboxProxyClass
open class GreetingClass : IGreetingClass {

    override val greeting: String = "Hello, World!"
    override var person: String = "Andrey"

    override fun sayHello() {
        println(greeting)
    }

    override suspend fun sayGoodbye() {
        println("Goodbye")
    }

    override suspend fun sumArguments(vararg args: Int): Int {
        return args.sum()
    }

    override suspend fun String.toPerson(): String {
        return "$this ,${person}"
    }

}