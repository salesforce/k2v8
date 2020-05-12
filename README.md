## K2V8

K2V8 is a de/serialization library for converting between Kotlin objects and [J2V8](https://github.com/eclipsesource/J2V8) V8Objects.

K2V8 uses [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization) for the serialization strategy.

### Usage

```kotlin
    @Serializable
    data class ExampleClass (
        val value: String
    )

    val fromObject = ExampleClass("foo")
    val v8 = V8.createV8Runtime()
    
    // Initialize K2V8 with an instance of the V8 Runtime
    val k2V8 = K2V8(Configuration(v8))
    val serializer = ExampleClass.serializer()
    
    // Call toV8 and fromV8 with the serializer to use and the source object
    val v8Object = k2V8.toV8(serializer, fromObject)
    val kotlinObject = k2V8.fromV8(serializer, v8Object)
```