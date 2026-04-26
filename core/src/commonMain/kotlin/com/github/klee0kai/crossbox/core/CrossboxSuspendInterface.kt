package com.github.klee0kai.crossbox.core

/**
 * Generates a suspend interface based on a synchronous class.
 *
 * Creates a coroutine-friendly version of class methods using the suspend keyword.
 * Suspend functions work with Kotlin coroutines and allow writing asynchronous code
 * in a synchronous style.
 *
 * ## Usage Examples
 *
 * ### Basic usage
 * ```kotlin
 * @CrossboxSuspendInterface
 * open class GreetingClass : IGreetingClass {
 *     override val greeting: String = "Hello, World!"
 *
 *     override fun sayHello() {
 *         println(greeting)
 *     }
 * }
 *
 * // Generated:
 * // suspend fun sayHello()
 * // suspend val greeting: String
 * ```
 *
 * ### Usage in coroutines
 * ```kotlin
 * @CrossboxSuspendInterface
 * open class DataService : IDataService {
 *     override fun fetchData(): List<String> {
 *         // Long operation
 *         return listOf("data1", "data2")
 *     }
 * }
 *
 * // Usage:
 * suspend fun main() {
 *     val service = DataService()
 *     // service will be automatically wrapped for suspend calls
 *     val data = service.fetchData()  // suspend function
 * }
 * ```
 *
 * ### With special parameters
 * ```kotlin
 * @CrossboxSuspendInterface
 * open class GreetingClass : IGreetingClass {
 *     override suspend fun sumArguments(vararg args: Int): Int {
 *         // Already a suspend function
 *         delay(100)
 *         return args.sum()
 *     }
 *
 *     override suspend fun String.toPerson(): String {
 *         // Extension suspend function
 *         delay(50)
 *         return this + " as Person"
 *     }
 * }
 *
 * // All functionality is preserved including varargs and extensions
 * ```
 *
 * ### With property generation disabled
 * ```kotlin
 * @CrossboxSuspendInterface(genProperties = false)
 * open class MyClass {
 *     override val name: String = "test"
 *     override fun doSomething() { }
 * }
 *
 * // Only suspend methods are generated, properties are skipped
 * ```
 *
 * ### Combined with async interface
 * ```kotlin
 * @CrossboxAsyncInterface
 * @CrossboxSuspendInterface
 * open class GreetingClass : IGreetingClass {
 *     override fun sayHello() { println("Hello") }
 * }
 *
 * // Generated:
 * // sayHelloAsync(): CompletableFuture<Unit>
 * // suspend fun sayHello()
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Class must be open**
 *    ```kotlin
 *    @CrossboxSuspendInterface
 *    open class MyClass {  // open is required!
 *        // ...
 *    }
 *    ```
 *
 * 2. **genProperties parameter (default: true)**
 *    - When true, suspend properties are generated
 *    - Properties are only available in coroutine context
 *
 * 3. **genFunctions parameter (default: true)**
 *    - When true, suspend methods are generated
 *    - Sync methods are wrapped in suspend calls
 *
 * 4. **Usage in coroutines**
 *    ```kotlin
 *    launch {
 *        val result = suspendMethod()  // Call without .await()
 *    }
 *    ```
 *    Simpler syntax than with CompletableFuture.
 *
 * 5. **Extension functions**
 *    - Extension functions are supported: `suspend fun String.doSomething()`
 *    - Receiver type is preserved
 *
 * 6. **Varargs parameters**
 *    - Varargs are supported: `suspend fun func(vararg args: Int)`
 *    - All parameters are preserved
 *
 * 7. **Exception handling in coroutines**
 *    ```kotlin
 *    try {
 *        val result = suspendMethod()
 *    } catch (e: Exception) {
 *        // Error handling
 *    }
 *    ```
 *    Exceptions are handled in the normal way.
 *
 * 8. **Dispatcher context**
 *    - Suspend functions execute in the current coroutine builder context
 *    - Dispatcher can be changed using `withContext(Dispatchers.IO) { }`
 *
 * 9. **Performance and ease of use**
 *    - Suspend functions are easier to use than callback approach
 *    - Consume less memory than full threads
 *    - Ideal for I/O operations and network requests
 *
 * 10. **Compatibility with async/await**
 *     ```kotlin
 *     val deferred = async { suspendMethod() }
 *     val result = deferred.await()
 *     ```
 *     Works without issues.
 *
 * 11. **Difference from @CrossboxAsyncInterface**
 *     - Suspend: uses coroutines, easier to use
 *     - Async: uses CompletableFuture, more low-level approach
 *     - Usually used together for different application contexts
 *
 * @param genProperties Generate suspend properties
 * @param genFunctions Generate suspend methods
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxSuspendInterface(
    val genProperties: Boolean = true,
    val genFunctions: Boolean = true,
)
