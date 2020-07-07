package com.salesforce.k2v8.internal

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.salesforce.k2v8.V8ObjectDecoder
import com.salesforce.k2v8.V8ObjectEncoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicKind
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.SerialKind
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.UnionKind
import kotlinx.serialization.internal.AbstractPolymorphicSerializer

/**
 * Adapted from [kotlinx.serialization.json.internal.encodePolymorphically]
 */
@InternalSerializationApi
@Suppress("UNCHECKED_CAST")
internal inline fun <T> V8ObjectEncoder.encodePolymorphically(serializer: SerializationStrategy<T>, value: T, ifPolymorphic: () -> Unit) {
    if (serializer !is AbstractPolymorphicSerializer<*>) {
        serializer.serialize(this, value)
        return
    }
    serializer as AbstractPolymorphicSerializer<Any> // PolymorphicSerializer <*> projects 2nd argument of findPolymorphic... to Nothing, so we need an additional cast
    val actualSerializer = serializer.findPolymorphicSerializer(this, value as Any).cast<Any>()
    validateIfSealed(serializer, actualSerializer, k2V8.configuration.classDiscriminator)
    val kind = actualSerializer.descriptor.kind
    checkKind(kind)
    ifPolymorphic()
    actualSerializer.serialize(this, value)
}

/**
 * Adapted from [kotlinx.serialization.json.internal.validateIfSealed]
 */
@InternalSerializationApi
private fun validateIfSealed(
    serializer: KSerializer<*>,
    actualSerializer: KSerializer<Any>,
    classDiscriminator: String
) {
    if (serializer !is SealedClassSerializer<*>) return
    if (classDiscriminator in actualSerializer.descriptor.cachedSerialNames()) {
        val baseName = serializer.descriptor.serialName
        val actualName = actualSerializer.descriptor.serialName
        error(
            "Sealed class '$actualName' cannot be serialized as base class '$baseName' because" +
                    " it has property name that conflicts with K2V8 class discriminator '$classDiscriminator'. " +
                    "You will need to change the class discriminator in K2V8 Configuration."
        )
    }
}

/**
 * Adapted from [kotlinx.serialization.json.internal.checkKind]
 */
internal fun checkKind(kind: SerialKind) {
    if (kind is UnionKind.ENUM_KIND) error("Enums cannot be serialized polymorphically with 'type' parameter.")
    if (kind is PrimitiveKind) error("Primitives cannot be serialized polymorphically with 'type' parameter.")
    if (kind is PolymorphicKind) error("Actual serializer for polymorphic cannot be polymorphic itself")
}

/**
 * Adapted from [kotlinx.serialization.json.internal.cast]
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
internal inline fun <T> KSerializer<*>.cast(): KSerializer<T> = this as KSerializer<T>

/**
 * Adapted from [kotlinx.serialization.json.internal.decodeSerializableValuePolymorphic]
 */
@InternalSerializationApi
internal fun <T> V8ObjectDecoder.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {

    // if this isn't a polymorphic serializer allow it to do it's own deserialization
    if (deserializer !is AbstractPolymorphicSerializer<*>) {
        return deserializer.deserialize(this)
    }

    // get current object and copy it, removing the type information
    return currentObject().use { original ->
        val type = original.getString(k2V8.configuration.classDiscriminator)
        val copied = V8Object(k2V8.configuration.runtime).apply {

            // filter out type key
            original.keys.filter { it != k2V8.configuration.classDiscriminator }.forEach { key ->
                when (original.getType(key)) {
                    V8Value.UNDEFINED -> addUndefined(key)
                    V8Value.NULL -> addNull(key)
                    V8Value.INTEGER -> add(key, original.getInteger(key))
                    V8Value.DOUBLE -> add(key, original.getDouble(key))
                    V8Value.BOOLEAN -> add(key, original.getBoolean(key))
                    V8Value.STRING -> add(key, original.getString(key))
                    V8Value.V8_ARRAY -> add(key, original.getArray(key))
                    V8Value.V8_OBJECT -> add(key, original.getObject(key))
                }
            }
        }

        // find the actual serializer for the type
        val actualSerializer = deserializer.findPolymorphicSerializer(this, type).cast<T>()

        // return deserialized object
        k2V8.fromV8(actualSerializer, copied)
    }
}