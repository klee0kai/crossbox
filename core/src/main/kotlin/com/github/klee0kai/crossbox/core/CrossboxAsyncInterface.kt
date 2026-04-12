package com.github.klee0kai.crossbox.core

/**
 * Generates an async interface based on a synchronous class.
 *
 * Creates async versions of class methods using callback approach or Future/Coroutines.
 * Useful for creating async versions of synchronous APIs.
 *
 * ## Usage Examples
 *
 * ### Basic usage
 * ```kotlin
 * @CrossboxAsyncInterface
 * open class GreetingClass : IGreetingClass {
 *     override val greeting: String = "Hello, World!"
 *
 *     override fun sayHello() {
 *         println(greeting)
 *     }
 * }
 *
 * // Generated IGreetingClassAsync interface with async methods
 * // Sync method sayHello() → async sayHelloAsync()
 * ```
 *
 * ### With special methods
 * ```kotlin
 * @CrossboxAsyncInterface
 * open class DataService : IDataService {
 *     override fun fetchData(): List<String> {
 *         // Long operation
 *         Thread.sleep(1000)
 *         return listOf("data1", "data2")
 *     }
 *
 *     override fun save(data: String) {
 *         // Long operation
 *         Thread.sleep(500)
 *     }
 * }
 *
 * // Generated:
 * // fetchDataAsync(): CompletableFuture<List<String>>
 * // saveAsync(data: String): CompletableFuture<Unit>
 * ```
 *
 * ### With function generation disabled
 * ```kotlin
 * @CrossboxAsyncInterface(genFunctions = false)
 * open class MyClass {
 *     override val property: String = "value"
 * }
 *
 * // Only async properties are generated
 * ```
 *
 * ### Combined with other annotations
 * ```kotlin
 * @CrossboxAsyncInterface
 * @CrossboxSuspendInterface
 * @CrossboxProxyClass
 * open class GreetingClass : IGreetingClass {
 *     override fun sayHello() {
 *         println("Hello")
 *     }
 * }
 *
 * // Async and suspend versions are generated simultaneously
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Class must be open**
 *    ```kotlin
 *    @CrossboxAsyncInterface
 *    open class MyClass {  // open is required!
 *        // ...
 *    }
 *    ```
 *
 * 2. **genProperties parameter (default: true)**
 *    - When true, async properties are generated
 *    - Properties are wrapped in CompletableFuture or async calls
 *
 * 3. **genFunctions parameter (default: true)**
 *    - When true, async methods are generated
 *    - Sync methods are converted to async
 *
 * 4. **Return types**
 *    - Method: `fun getData(): String` → `fun getDataAsync(): CompletableFuture<String>`
 *    - Method: `fun doWork()` → `fun doWorkAsync(): CompletableFuture<Unit>`
 *    - Method parameters remain the same
 *
 * 5. **Multi-threaded environment**
 *    - Async methods execute work in separate threads
 *    - Results are returned via CompletableFuture
 *    - No blocking of the current thread
 *
 * 6. **Exception handling**
 *    - Exceptions thrown in async methods are caught
 *    - Passed via CompletableFuture.completeExceptionally()
 *
 * 7. **Suspend functions**
 *    - If the original method is suspend, it's converted to an async call
 *    - Runs in the appropriate Dispatcher
 *
 * 8. **Performance**
 *    - Async operations avoid thread blocking
 *    - Recommended for I/O operations and network requests
 *
 * 9. **Testing**
 *    - Async code can be tested using `get()` on CompletableFuture
 *    - Or use `runBlocking` for synchronous waiting
 *
 * @param genProperties Generate async properties
 * @param genFunctions Generate async methods
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxAsyncInterface(
    val genProperties: Boolean = true,
    val genFunctions: Boolean = true,
)
