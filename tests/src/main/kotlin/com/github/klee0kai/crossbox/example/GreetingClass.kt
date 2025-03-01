package com.github.klee0kai.crossbox.example

import com.github.klee0kai.crossbox.core.CrossboxGenInterface
import com.github.klee0kai.crossbox.core.CrossboxNotSuspendInterface
import com.github.klee0kai.crossbox.core.CrossboxProxyClass
import com.github.klee0kai.crossbox.core.CrossboxSuspendInterface
import com.github.klee0kai.crossbox.example.crossbox.IGreetingClass

@CrossboxGenInterface
@CrossboxSuspendInterface
@CrossboxNotSuspendInterface
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

    override fun sumArguments(vararg args: Int): Int {
        return args.sum()
    }

    override fun String.toPerson(): String {
        return "$this ,${person}"
    }

}