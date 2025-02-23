package com.github.klee0kai.crossbox.tests

import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.fieldList
import org.junit.jupiter.api.Test

class SimpleModelsTests {

    @Test
    fun test1() {
        val list = SimpleModel.fieldList
//        println("hello world ${list}")
        assert(true)
    }

}