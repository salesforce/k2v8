package com.salesforce.k2v8.internal

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.salesforce.k2v8.V8DecodingException
import com.salesforce.k2v8.V8ObjectDecoder
import com.salesforce.k2v8.V8ObjectEncoder
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.internal.AbstractPolymorphicSerializer

/**
 * Adapted from [kotlinx.serialization.json.internal.encodePolymorphically]
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <T> V8ObjectEncoder.encodePolymorphically(serializer: SerializationStrategy<T>, value: T, ifPolymorphic: () -> Unit) {
    if (serializer !is AbstractPolymorphicSerializer<*>) {
        serializer.serialize(this, value)
        return
    }
    val actualSerializer = findActualSerializer(serializer as SerializationStrategy<Any>, value as Any)
    ifPolymorphic()
    actualSerializer.serialize(this, value)
}

/**
 * Adapted from [kotlinx.serialization.json.internal.findActualSerializer]
 */
private fun V8ObjectEncoder.findActualSerializer(
        serializer: SerializationStrategy<Any>,
        value: Any
): SerializationStrategy<Any> {
    val casted = serializer as AbstractPolymorphicSerializer<Any>
    val actualSerializer = casted.findPolymorphicSerializer(this, value )
    validateIfSealed(casted, actualSerializer, k2V8.configuration.classDiscriminator)
    val kind = actualSerializer.descriptor.kind
    checkKind(kind)
    return actualSerializer
}

/**
 * Adapted from [kotlinx.serialization.json.internal.validateIfSealed]
 */
private fun validateIfSealed(
    serializer: SerializationStrategy<*>,
    actualSerializer: SerializationStrategy<Any>,
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
    if (kind is SerialKind.ENUM) error("Enums cannot be serialized polymorphically with 'type' parameter.")
    if (kind is PrimitiveKind) error("Primitives cannot be serialized polymorphically with 'type' parameter.")
    if (kind is PolymorphicKind) error("Actual serializer for polymorphic cannot be polymorphic itself")
}

/**
 * Adapted from [kotlinx.serialization.json.internal.decodeSerializableValuePolymorphic]
 */
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
        val actualSerializer = deserializer.findPolymorphicSerializerOrNull(this, type) ?:
                throwSerializerNotFound(type, original)

        // return deserialized object
        @Suppress("UNCHECKED_CAST")
        k2V8.fromV8(actualSerializer as DeserializationStrategy<T>, copied)
    }
}

private fun throwSerializerNotFound(type: String?, v8Object: V8Object): Nothing {
    val suffix =
            if (type == null) "missing class discriminator ('null')"
            else "class discriminator '$type'"
    throw V8DecodingException("Polymorphic serializer was not found for $suffix, V8Object: $v8Object")
}
