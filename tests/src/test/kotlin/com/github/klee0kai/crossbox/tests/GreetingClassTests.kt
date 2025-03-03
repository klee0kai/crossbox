package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.core.proxies.SynchronizedProxy
import com.github.klee0kai.crossbox.example.GreetingClass
import com.github.klee0kai.crossbox.example.crossbox.IGreetingClassCrossboxProxy
import com.github.klee0kai.crossbox.example.crossbox.IGreetingClassToAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GreetingClassTests {

    @Test
    fun sayHelloSyncronizedProxy() = runBlocking {
        val greetingClassProxy = IGreetingClassCrossboxProxy(
            crossboxOrigin = GreetingClass(),
            crossBoxProxyProcessor = SynchronizedProxy(),
        )
        repeat(3) {
            launch {
                greetingClassProxy.sayHello()
            }
        }
    }


    @Test
    fun sayGoodbyeAsync() = runBlocking {
        val greetingClassAsync = IGreetingClassToAsync(
            crossboxOrigin = GreetingClass(),
            crossboxScope = CoroutineScope(SupervisorJob()),
        )
        greetingClassAsync.sayGoodbye().join()
    }

}