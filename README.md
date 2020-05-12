## K2V8

K2V8 is an de/serialization library for converting between Kotlin objects and [J2V8](https://github.com/eclipsesource/J2V8) V8Objects.

K2V8 uses [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization) for the serialization stratgy.

### Usage

```kotlin
    @Serializable
    data class ExampleClass(
        val value: String
    )

    val fromObject = ExampleClass("val")
    val v8 = V8.createV8Runtime()
    
    // Initialize K2V8 with an instance of the V8 Runtime
    val k2V8 = K2V8(Configuration(v8))
    
    // Call convert with the source object and the serializer to use
    val v8Object = k2V8.convertToV8Object(fromObject, ExampleClass.serializer())
    val kotlinObject = k2V8.convertFromV8Object(v8Object, ExampleClass.serializer())
```