package com.github.klee0kai.crossbox.processor.poet

val String.crossboxPackageName: String
    get() {
        return if (endsWith(".crossbox")) this
        else "$this.crossbox"
    }