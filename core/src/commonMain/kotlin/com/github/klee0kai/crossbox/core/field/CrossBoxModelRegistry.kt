package com.github.klee0kai.crossbox.core.field

import kotlin.reflect.KClass

interface CrossBoxModelRegistry {

    val modelRegistry: Map<KClass<*>, ModelTool<*>>

    open class ModelTool<T : Any>(
        val type: KClass<T>,
        val crossboxFieldList: ((`_`: T) -> List<FieldInfo<T>>)?,
        val merge: ((obj: T, pair: T?) -> T),
        val deepMerge: ((obj: T, pair: T?) -> T),
    )

}

inline fun <reified T> CrossBoxModelRegistry.crossboxFieldList(value: T): List<FieldInfo<T>> {
    val lambda = modelRegistry[T::class]!!.crossboxFieldList as ((`_`: T) -> List<FieldInfo<T>>)
    return lambda.invoke(value)
}

inline fun <reified T> CrossBoxModelRegistry.merge(obj1: T, obj2: T?): T {
    val lambda = modelRegistry[T::class]!!.merge as ((obj: T, pair: T?) -> T)?
    return lambda!!.invoke(obj1, obj2)
}

inline fun <reified T> CrossBoxModelRegistry.deepMerge(obj1: T, obj2: T?): T {
    val lambda = modelRegistry[T::class]!!.deepMerge as ((obj: T, pair: T?) -> T)?
    return lambda!!.invoke(obj1, obj2)
}