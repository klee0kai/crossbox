package com.github.klee0kai.crossbox.core

/**
 * Generates a base interface based on a class to define a contract.
 *
 * Creates an interface that describes all public properties and methods of the class.
 * This allows working with the class through an interface, which facilitates testing and
 * creating alternative implementations.
 *
 * ## Usage Examples
 *
 * ### Basic usage
 * ```kotlin
 * @CrossboxGenInterface
 * open class GreetingClass : IGreetingClass {
 *     override val greeting: String = "Hello, World!"
 *     override var person: String = "Andrey"
 *
 *     override fun sayHello() {
 *         println(greeting)
 *     }
 * }
 *
 * // Generated interface:
 * // interface IGreetingClass {
 * //     val greeting: String
 * //     var person: String
 * //     fun sayHello()
 * // }
 * ```
 *
 * ### For creating mocks in tests
 * ```kotlin
 * @CrossboxGenInterface
 * open class DataService : IDataService {
 *     override fun fetchData(): List<String> = listOf("data1", "data2")
 *     override fun save(data: String) { /* ... */ }
 * }
 *
 * // Usage in tests:
 * val mockService: IDataService = mockk<IDataService>()
 * every { mockService.fetchData() } returns listOf("test")
 * ```
 *
 * ### With special methods
 * ```kotlin
 * @CrossboxGenInterface
 * open class GreetingClass : IGreetingClass {
 *     override suspend fun sayGoodbye() {
 *         println("Goodbye")
 *     }
 *
 *     override suspend fun sumArguments(vararg args: Int): Int {
 *         return args.sum()
 *     }
 *
 *     override suspend fun String.toPerson(): String {
 *         return "\$this ,${person}"
 *     }
 * }
 *
 * // Interface preserves all modifiers and parameter types
 * ```
 *
 * ### With property generation disabled
 * ```kotlin
 * @CrossboxGenInterface(genProperties = false)
 * open class MyClass : IMyClass {
 *     override val name: String = "test"
 *     override fun doSomething() { }
 * }
 *
 * // Interface will contain only methods
 * ```
 *
 * ### Combined with proxy annotations
 * ```kotlin
 * @CrossboxGenInterface
 * @CrossboxProxyClass
 * @CrossboxAsyncInterface
 * @CrossboxSuspendInterface
 * open class GreetingClass : IGreetingClass {
 *     override fun sayHello() { println("Hello") }
 * }
 *
 * // Generated:
 * // 1. Interface IGreetingClass
 * // 2. Proxy class
 * // 3. Async methods
 * // 4. Suspend methods
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Class must be open**
 *    ```kotlin
 *    @CrossboxGenInterface
 *    open class MyClass {  // open is required!
 *        // ...
 *    }
 *    ```
 *
 * 2. **genProperties parameter (default: true)**
 *    - When true, properties are generated in the interface
 *    - Val and var properties are included in the interface
 *
 * 3. **genFunctions parameter (default: true)**
 *    - When true, methods are generated in the interface
 *    - All public methods are included in the interface
 *
 * 4. **Only public members**
 *    ```kotlin
 *    @CrossboxGenInterface
 *    open class MyClass {
 *        val publicProp = "visible"       // Will be included in interface
 *        private val privateProp = "hidden"  // Won't be included
 *        protected fun protectedMethod() { }  // Won't be included (by default)
 *    }
 *    ```
 *
 * 5. **Generated interface name**
 *    - For class `MyClass`, interface `IMyClass` is generated
 *    - Class must already implement this interface
 *
 * 6. **Extension functions**
 *    - Extension functions in the interface are supported
 *    - Example: `suspend fun String.toPerson(): String`
 *
 * 7. **Varargs and default parameters**
 *    - Varargs parameters are supported
 *    - Default parameters are preserved
 *
 * 8. **Function modifiers**
 *    - `suspend` modifier is preserved in the interface
 *    - `infix` modifier is preserved
 *    - `operator` modifier is preserved
 *
 * 9. **Usage in architecture**
 *    - Ideal for dependency injection
 *    - Allows injecting mocked implementations in tests
 *    - Simplifies implementation updates without affecting clients
 *
 * 10. **Parameter and return types**
 *     - All types are included as-is
 *     - Nullable types are preserved: `String?` remains `String?`
 *     - Generic types are preserved: `List<String>` remains `List<String>`
 *
 * 11. **Combination with other annotations**
 *     - @CrossboxGenInterface is typically used first
 *     - Other annotations extend the functionality of the created interface
 *
 * @param genProperties Generate properties in the interface
 * @param genFunctions Generate methods in the interface
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxGenInterface(
    val genProperties: Boolean = true,
    val genFunctions: Boolean = true,
)
