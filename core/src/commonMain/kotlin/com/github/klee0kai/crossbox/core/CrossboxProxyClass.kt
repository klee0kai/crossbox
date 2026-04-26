package com.github.klee0kai.crossbox.core

/**
 * Generates a proxy class to wrap the original class.
 *
 * The proxy pattern allows adding additional behavior to an object without changing its interface.
 * Used together with other annotations to create various functional variations.
 *
 * Generates a proxy class that:
 * - Wraps the original class
 * - Proxies all properties and methods
 * - Can be extended to add additional logic
 *
 * ## Usage Examples
 *
 * ### Basic proxy creation
 * ```kotlin
 * @CrossboxProxyClass
 * open class GreetingClass : IGreetingClass {
 *     override val greeting: String = "Hello, World!"
 *
 *     override fun sayHello() {
 *         println(greeting)
 *     }
 * }
 *
 * // Generated:
 * // class GreetingClassProxy(private val instance: GreetingClass) : GreetingClass
 * ```
 *
 * ### With property generation disabled
 * ```kotlin
 * @CrossboxProxyClass(genProperties = false)
 * open class MyClass : IMyClass {
 *     override val name: String = "test"
 *     override fun doSomething() { }
 * }
 *
 * // Only methods are generated, properties are skipped
 * ```
 *
 * ### With function generation disabled
 * ```kotlin
 * @CrossboxProxyClass(genFunctions = false)
 * open class MyClass : IMyClass {
 *     override val name: String = "test"
 *     override fun doSomething() { }
 * }
 *
 * // Only properties are generated, methods are skipped
 * ```
 *
 * ## Usage Notes
 *
 * 1. **Class must be open**
 *    ```kotlin
 *    @CrossboxProxyClass
 *    open class MyClass {  // open is required!
 *        // ...
 *    }
 *    ```
 *    This allows the generated code to extend the class.
 *
 * 2. **genProperties parameter (default: true)**
 *    - When true, proxy properties are generated
 *    - Useful to disable if properties require special handling
 *
 * 3. **genFunctions parameter (default: true)**
 *    - When true, proxy methods are generated
 *    - Useful to disable if methods require special handling
 *
 * 4. **Usually used with other annotations**
 *    ```kotlin
 *    @CrossboxProxyClass
 *    @CrossboxAsyncInterface
 *    @CrossboxSuspendInterface
 *    @CrossboxGenInterface
 *    open class MyClass : IMyInterface {
 *        // ...
 *    }
 *    ```
 *    Each annotation adds its own functionality to the proxy.
 *
 * 5. **Interfaces to implement**
 *    - It's recommended to implement an explicit interface (inherit)
 *    - This helps the KSP processor understand the class contract
 *
 * 6. **Private fields and methods**
 *    - Private class members will not be proxied
 *    - Only public elements are included in the proxy
 *
 * 7. **Companion object**
 *    - Companion object and its members are not proxied
 *    - These are static members, they remain in the original class
 *
 * 8. **Interaction with other annotations**
 *    - @CrossboxAsyncInterface - generates async versions of methods
 *    - @CrossboxSuspendInterface - generates suspend versions of methods
 *    - @CrossboxGenInterface - generates base interface
 *    - All work together on a single proxy class
 *
 * 9. **Inheritance and overriding**
 *    - The generated proxy can be overridden
 *    - Multiple interface inheritance is supported
 *
 * @param genProperties Generate proxy for properties
 * @param genFunctions Generate proxy for methods
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CrossboxProxyClass(
    val genProperties: Boolean = true,
    val genFunctions: Boolean = true,
)
