/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [K2V8].
 */
@RunWith(AndroidJUnit4::class)
class K2V8Test: K2V8TestBase() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun supportedTypesToV8() = v8.scope {
        forAll(
                100,
                Gen.list(Gen.char()).filter { it.isNotEmpty() },
                Gen.list(Gen.long()).filter { it.isNotEmpty() },
                Gen.list(Gen.string()).filter { it.isNotEmpty() },
                Gen.list(Gen.enum<Enum>()).filter { it.isNotEmpty() },
                Gen.map(Gen.string(), Gen.string().filter { it.isNotEmpty() })
        ) { chars, longs, strings, enums, stringMap ->
            val supportedTypes = getSupportedTypes(chars, longs, strings, enums, stringMap)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(SupportedTypes.serializer(), supportedTypes)) {
                // verify the k2V8.toV8 only increased the reference count by 1 - the final result object
                (v8.objectReferenceCount - refCountStart) == 1L &&
                        getInteger("byte").toByte() == supportedTypes.byte &&
                        get("nullByte") == supportedTypes.nullByte &&
                        getInteger("nonNullByte").toByte() == supportedTypes.nonNullByte &&
                        getInteger("short").toShort() == supportedTypes.short &&
                        get("nullShort") == supportedTypes.nullShort &&
                        getInteger("nonNullShort").toShort() == supportedTypes.nonNullShort &&
                        getInteger("char").toChar() == supportedTypes.char &&
                        get("nullChar") == supportedTypes.nullChar &&
                        getInteger("nonNullChar").toChar() == supportedTypes.nonNullChar &&
                        getInteger("int") == supportedTypes.int &&
                        get("nullInt") == supportedTypes.nullInt &&
                        getInteger("nonNullInt") == supportedTypes.nonNullInt &&
                        getDouble("long") == supportedTypes.long.toDouble() &&
                        get("nullLong") == supportedTypes.nullLong &&
                        getDouble("nonNullLong") == supportedTypes.nonNullLong!!.toDouble() &&
                        getDouble("double") == supportedTypes.double &&
                        get("nullDouble") == supportedTypes.nullDouble &&
                        getDouble("nonNullDouble") == supportedTypes.nonNullDouble &&
                        getDouble("float").toFloat() == supportedTypes.float &&
                        get("nullFloat") == supportedTypes.nullFloat &&
                        getDouble("nonNullFloat").toFloat() == supportedTypes.nonNullFloat &&
                        getString("string") == supportedTypes.string &&
                        get("nullString") == supportedTypes.nullString &&
                        getString("nonNullString") == supportedTypes.nonNullString &&
                        getBoolean("boolean") == supportedTypes.boolean &&
                        get("nullBoolean") == supportedTypes.nullBoolean &&
                        getBoolean("nonNullBoolean") == supportedTypes.nonNullBoolean &&
                        getString("enum") == supportedTypes.enum.name &&
                        get("nullEnum") == supportedTypes.nullEnum &&
                        getObject("unit").isUndefined &&
                        getObject("nestedObject").getString("value") == supportedTypes.nestedObject.value &&
                        getObject("nullNestedObject") == null &&
                        getObject("nonNullNestedObject").getString("value") == supportedTypes.nonNullNestedObject!!.value &&
                        getObject("doubleNestedObject").getObject("nestedObject")
                                .getString("value") == supportedTypes.doubleNestedObject.nestedObject.value &&
                        with(getObject("byteList") as V8Array) {
                            supportedTypes.byteList.valueAtIndex { getInteger(it).toByte() }
                        } &&
                        with(getObject("shortList") as V8Array) {
                            supportedTypes.shortList.valueAtIndex { getInteger(it).toShort() }
                        } &&
                        with(getObject("charList") as V8Array) {
                            supportedTypes.charList.valueAtIndex { getInteger(it).toChar() }
                        } &&
                        with(getObject("intList") as V8Array) {
                            supportedTypes.intList.valueAtIndex { getInteger(it) }
                        } &&
                        with(getObject("longList") as V8Array) {
                            supportedTypes.longList.valueAtIndex({ it.toDouble() }) { getDouble(it) }
                        } &&
                        with(getObject("floatList") as V8Array) {
                            supportedTypes.floatList.valueAtIndex { getDouble(it).toFloat() }
                        } &&
                        with(getObject("doubleList") as V8Array) {
                            supportedTypes.doubleList.valueAtIndex { getDouble(it) }
                        } &&
                        with(getObject("stringList") as V8Array) {
                            supportedTypes.stringList.valueAtIndex { getString(it) }
                        } &&
                        with(getObject("booleanList") as V8Array) {
                            supportedTypes.booleanList.valueAtIndex { getBoolean(it) }
                        } &&
                        with(getObject("enumList") as V8Array) {
                            supportedTypes.enumList.valueAtIndex({ it.name }) { getString(it) }
                        } &&
                        with(getObject("nestedObjectList") as V8Array) {
                            supportedTypes.nestedObjectList.valueAtIndex({ it.value }) {
                                getObject(it).getString("value")
                            }
                        } &&
                        with(getObject("stringMap") as V8Object) {
                            supportedTypes.stringMap.valueForKey { getString(it) }
                        } &&
                        with(getObject("enumMap") as V8Object) {
                            supportedTypes.enumMap.valueForKey({ it.name }) { getString(it) }
                        }
            }
        }
    }

    @Test
    fun supportedTypesFromV8() = v8.scope {
        forAll(
                100,
                Gen.list(Gen.char()).filter { it.isNotEmpty() },
                Gen.list(Gen.long()).filter { it.isNotEmpty() },
                Gen.list(Gen.string()).filter { it.isNotEmpty() },
                Gen.list(Gen.enum<Enum>()).filter { it.isNotEmpty() },
                Gen.map(Gen.string(), Gen.string().filter { it.isNotEmpty() })
        ) { chars, longs, strings, enums, stringMap ->
            val supportedTypes = getSupportedTypes(chars, longs, strings, enums, stringMap)
            val nestedV8Object = V8Object(v8).apply {
                add("value", supportedTypes.nestedObject.value)
            }
            val nonNullNestedV8Object = V8Object(v8).apply {
                add("value", supportedTypes.nonNullNestedObject!!.value)
            }
            val doubleNestedVObject = V8Object(v8).apply {
                add(
                        "nestedObject",
                        V8Object(v8).add("value", supportedTypes.doubleNestedObject.nestedObject.value)
                )
            }
            val byteV8Array = supportedTypes.byteList.map { it.toInt() }.toV8Array(v8)
            val shortV8Array = supportedTypes.shortList.map { it.toInt() }.toV8Array(v8)
            val charV8Array = supportedTypes.charList.map { it.toInt() }.toV8Array(v8)
            val intV8Array = supportedTypes.intList.toV8Array(v8)
            val longV8Array = supportedTypes.longList.map { it.toDouble() }.toV8Array(v8)
            val floatV8Array = supportedTypes.floatList.map { it.toDouble() }.toV8Array(v8)
            val doubleV8Array = supportedTypes.doubleList.toV8Array(v8)
            val stringV8Array = supportedTypes.stringList.toV8Array(v8)
            val booleanV8Array = supportedTypes.booleanList.toV8Array(v8)
            val enumV8Array = supportedTypes.enumList.map { it.name }.toV8Array(v8)
            val nestedObjectV8Array = V8Array(v8).apply {
                supportedTypes.nestedObjectList.forEach {
                    push(V8Object(v8).also { obj -> obj.add("value", it.value) })
                }
            }
            val stringStringV8Object = supportedTypes.stringMap.toV8Object(v8)
            val enumStringV8Array = V8Array(v8).apply {
                supportedTypes.enumMap.entries.forEach { (key, value) -> add(key.name, value) }
            }
            val value = V8Object(v8).apply {
                add("byte", supportedTypes.byte.toInt())
                addNull("nullByte")
                add("nonNullByte", supportedTypes.nonNullByte!!.toInt())
                add("short", supportedTypes.short.toInt())
                addNull("nullShort")
                add("nonNullShort", supportedTypes.nonNullShort!!.toInt())
                add("char", supportedTypes.char.toInt())
                addNull("nullChar")
                add("nonNullChar", supportedTypes.nonNullChar!!.toInt())
                add("int", supportedTypes.int)
                addNull("nullInt")
                add("nonNullInt", supportedTypes.nonNullInt!!)
                add("long", supportedTypes.long.toDouble())
                addNull("nullLong")
                add("nonNullLong", supportedTypes.nonNullLong!!.toDouble())
                add("double", supportedTypes.double)
                addNull("nullDouble")
                add("nonNullDouble", supportedTypes.nonNullDouble!!)
                add("float", supportedTypes.float.toDouble())
                addNull("nullFloat")
                add("nonNullFloat", supportedTypes.nonNullFloat!!.toDouble())
                add("string", supportedTypes.string)
                addNull("nullString")
                add("nonNullString", supportedTypes.nonNullString)
                add("boolean", supportedTypes.boolean)
                addNull("nullBoolean")
                add("nonNullBoolean", supportedTypes.nonNullBoolean!!)
                add("enum", supportedTypes.enum.name)
                addNull("nullEnum")
                addUndefined("unit")
                add("nestedObject", nestedV8Object)
                addNull("nullNestedObject")
                add("nonNullNestedObject", nonNullNestedV8Object)
                add("doubleNestedObject", doubleNestedVObject)
                add("byteList", byteV8Array)
                add("shortList", shortV8Array)
                add("charList", charV8Array)
                add("intList", intV8Array)
                add("longList", longV8Array)
                add("doubleList", doubleV8Array)
                add("floatList", floatV8Array)
                add("stringList", stringV8Array)
                add("booleanList", booleanV8Array)
                add("enumList", enumV8Array)
                add("nestedObjectList", nestedObjectV8Array)
                add("stringMap", stringStringV8Object)
                add("enumMap", enumStringV8Array)
            }
            val startCount = v8.objectReferenceCount
            with(k2V8.fromV8(SupportedTypes.serializer(), value)) {
                // make sure the V8Object references all released.
                v8.objectReferenceCount - startCount == 0L &&
                        byte == supportedTypes.byte &&
                        nullByte == supportedTypes.nullByte &&
                        nonNullByte == supportedTypes.nonNullByte &&
                        short == supportedTypes.short &&
                        nullShort == supportedTypes.nullShort &&
                        nonNullShort == supportedTypes.nonNullShort &&
                        char == supportedTypes.char &&
                        nullChar == supportedTypes.nullChar &&
                        nonNullChar == supportedTypes.nonNullChar &&
                        int == supportedTypes.int &&
                        nullInt == supportedTypes.nullInt &&
                        nonNullInt == supportedTypes.nonNullInt &&
                        long.toDouble() == supportedTypes.long.toDouble() &&
                        nullLong == supportedTypes.nullLong &&
                        nonNullLong!!.toDouble() == supportedTypes.nonNullLong!!.toDouble() &&
                        double == supportedTypes.double &&
                        nullDouble == supportedTypes.nullDouble &&
                        nonNullDouble == supportedTypes.nonNullDouble &&
                        float == supportedTypes.float &&
                        nullFloat == supportedTypes.nullFloat &&
                        nonNullFloat == supportedTypes.nonNullFloat &&
                        string == supportedTypes.string &&
                        nullString == supportedTypes.nullString &&
                        nonNullString == supportedTypes.nonNullString &&
                        boolean == supportedTypes.boolean &&
                        nullBoolean == supportedTypes.nullBoolean &&
                        nonNullBoolean == supportedTypes.nonNullBoolean &&
                        enum == supportedTypes.enum &&
                        nullEnum == supportedTypes.nullEnum &&
                        unit == supportedTypes.unit &&
                        nestedObject.value == supportedTypes.nestedObject.value &&
                        nullNestedObject == null &&
                        nonNullNestedObject!!.value == supportedTypes.nonNullNestedObject!!.value &&
                        byteList == supportedTypes.byteList &&
                        shortList == supportedTypes.shortList &&
                        charList == supportedTypes.charList &&
                        intList == supportedTypes.intList &&
                        longList.map { it.toDouble() } == supportedTypes.longList.map { it.toDouble() } &&
                        floatList == supportedTypes.floatList &&
                        doubleList == supportedTypes.doubleList &&
                        stringList == supportedTypes.stringList &&
                        booleanList == supportedTypes.booleanList &&
                        enumList == supportedTypes.enumList &&
                        nestedObjectList == supportedTypes.nestedObjectList &&
                        stringMap == supportedTypes.stringMap &&
                        enumMap == supportedTypes.enumMap
            }
        }
    }

    // region sealed class tests

    @Test
    fun sealedClassToV8() = v8.scope {
        forAll(100, Gen.string()) { string ->
            val refCountStart = v8.objectReferenceCount
            val encoded = k2V8.toV8(SealedClass.serializer(), SealedClass.ClassOne(string))
            assertEquals(refCountStart + 1, v8.objectReferenceCount)
            with(encoded) {
                getString("someString") == string
            }
        }
    }

    @Test
    fun sealedClassFromV8() = v8.scope {
        forAll(100, Gen.string()) { string ->
            val refCountStart = v8.objectReferenceCount
            val decoded = k2V8.fromV8(SealedClass.serializer(), k2V8.toV8(SealedClass.serializer(), SealedClass.ClassOne(string)))
            assertEquals(1, v8.objectReferenceCount - refCountStart)
            decoded is SealedClass.ClassOne && decoded.someString == string
        }
    }

    // endregion

    // region list tests

    @Test
    fun byteListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.byte())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Byte.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toInt() }) { getInteger(it) }
            }
        }
    }

    @Test
    fun byteListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.byte())) { list ->
            val array = list.map { it.toInt() }.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Byte.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun shortListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.short())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Short.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toInt() }) { getInteger(it) }
            }
        }
    }

    @Test
    fun shortListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.short())) { list ->
            val array = list.map { it.toInt() }.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Short.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun charListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.char())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Char.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toInt() }) { getInteger(it) }
            }
        }
    }

    @Test
    fun charListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.char())) { list ->
            val array = list.map { it.toInt() }.toV8Array(v8)
            with(k2V8.fromV8(ListSerializer(Char.serializer()), array)) {
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun intListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.int())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Int.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { getInteger(it) }
            }
        }
    }

    @Test
    fun intListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.int())) { list ->
            val array = list.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Int.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun longListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.long())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Long.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toDouble() }) { getDouble(it) }
            }
        }
    }

    @Test
    fun longListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.long())) { list ->
            val array = list.map { it.toDouble() }.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Long.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toDouble() }) { get(it).toDouble() }
            }
        }
    }

    @Test
    fun floatListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.float().filter { !it.isNaN() })) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Float.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.toDouble() }) { getDouble(it) }
            }
        }
    }

    @Test
    fun floatListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.float().filter { !it.isNaN() })) { list ->
            val array = list.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Float.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun doubleListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.double().filter { !it.isNaN() })) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Double.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { getDouble(it) }
            }
        }
    }

    @Test
    fun doubleListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.double().filter { !it.isNaN() })) { list ->
            val array = list.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Double.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun stringListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.string())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(String.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { getString(it) }
            }
        }
    }

    @Test
    fun stringListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.string())) { list ->
            val array = list.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(String.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun booleanListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.bool())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Boolean.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { getBoolean(it) }
            }
        }
    }

    @Test
    fun booleanListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.bool())) { list ->
            val array = list.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Boolean.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun enumListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.enum<Enum>())) { list ->
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(Enum.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.name }) { getString(it) }
            }
        }
    }


    @Test
    fun enumListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.enum<Enum>())) { list ->
            val array = list.map { it.toString() }.toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(Enum.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it) }
            }
        }
    }

    @Test
    fun objectListToV8() = v8.scope {
        forAll(100, Gen.list(Gen.string())) { strings ->
            val list = strings.map { NestedObject(it) }
            val refCountStart = v8.objectReferenceCount
            with(k2V8.toV8(ListSerializer(NestedObject.serializer()), list) as V8Array) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex({ it.value }) { getObject(it).getString("value") }
            }
        }
    }

    @Test
    fun objectListFromV8() = v8.scope {
        forAll(100, Gen.list(Gen.string())) { list ->
            val array = list
                    .map { value ->
                        V8Object(v8).also {
                            it.add("value", value)
                        }
                    }
                    .toV8Array(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(ListSerializer(NestedObject.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                list.valueAtIndex { get(it).value }
            }
        }
    }

    // endregion

    // region map tests

    @Test
    fun stringKeyedMapToV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.string())) { map ->
            val refCountStart = v8.objectReferenceCount
            with(
                    k2V8.toV8(
                            MapSerializer(String.serializer(), String.serializer()),
                            map
                    )
            ) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                map.valueForKey { getString(it) }
            }
        }
    }

    @Test
    fun stringKeyedMapFromV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.string())) { map ->
            val v8Object = map.toV8Object(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(MapSerializer(String.serializer(), String.serializer()), v8Object)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                map.valueForKey { get(it) }
            }
        }
    }

    @Test
    fun stringKeyedIntValueMapToV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.int())) { map ->
            val refCountStart = v8.objectReferenceCount
            with(
                    k2V8.toV8(
                            MapSerializer(String.serializer(), Int.serializer()),
                            map
                    )
            ) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                map.valueForKey { getInteger(it) }
            }
        }
    }

    @Test
    fun stringKeyedIntValueMapFromV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.int())) { map ->
            val v8Object = map.toV8Object(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(MapSerializer(String.serializer(), Int.serializer()), v8Object)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                map.valueForKey { get(it) }
            }
        }
    }

    @Test
    fun stringKeyedDoubleValueMapToV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.double())) { map ->
            val refCountStart = v8.objectReferenceCount
            with(
                    k2V8.toV8(
                            MapSerializer(String.serializer(), Double.serializer()),
                            map
                    )
            ) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                map.valueForKey { getDouble(it) }
            }
        }
    }

    @Test
    fun stringKeyedDoubleValueMapFromV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.double())) { map ->
            val v8Object = map.toV8Object(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(MapSerializer(String.serializer(), Double.serializer()), v8Object)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                map.valueForKey { get(it) }
            }
        }
    }

    @Test
    fun stringKeyedSerializableValueMapToV8() = v8.scope {
        forAll(5, Gen.map(Gen.string(), Gen.string())) { stringMap ->
            val map = stringMap.mapValues { (_, value) ->
                NestedObject(value)
            }
            val refCountStart = v8.objectReferenceCount
            with(
                    k2V8.toV8(
                            MapSerializer(String.serializer(), NestedObject.serializer()),
                            map
                    )
            ) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                map.valueForKey {
                    NestedObject(getObject(it).getString("value"))
                }
            }
        }
    }

    @Test
    fun stringKeyedSerializableValueMapFromV8() = v8.scope {
        forAll(100, Gen.map(Gen.string(), Gen.string())) { map ->
            val v8Object = map.mapValues { (_, value) ->
                V8Object(v8).also {
                    it.add("value", value)
                }
            }.toV8Object(v8)
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(MapSerializer(String.serializer(), NestedObject.serializer()), v8Object)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                map.valueForKey { get(it)!!.value }
            }
        }
    }

    @Test
    fun enumKeyedMapToV8() = v8.scope {
        forAll(100, Gen.map(Gen.enum<Enum>(), Gen.string())) { map ->
            val refCountStart = v8.objectReferenceCount
            with(
                    k2V8.toV8(
                            MapSerializer(Enum.serializer(), String.serializer()),
                            map
                    )
            ) {
                assertEquals(1, v8.objectReferenceCount - refCountStart)
                map.valueForKey({ it.name }) { getString(it) }
            }
        }
    }

    @Test
    fun enumKeyedMapFromV8() = v8.scope {
        forAll(100, Gen.map(Gen.enum<Enum>(), Gen.string())) { map ->
            val array = V8Array(v8).apply {
                map.entries.onEach { (key, value) -> add(key.name, value) }
            }
            val refCountStart = v8.objectReferenceCount
            with(k2V8.fromV8(MapSerializer(Enum.serializer(), String.serializer()), array)) {
                assertEquals(0, v8.objectReferenceCount - refCountStart)
                map.valueForKey { get(it) }
            }
        }
    }

    @Test(expected = V8EncodingException::class)
    fun intKeyedMapToV8ThrowsException() {
        val intMap = mapOf(1 to "1", 2 to "2", 3 to "3")
        val refCountStart = v8.objectReferenceCount
        k2V8.toV8(MapSerializer(Int.serializer(), String.serializer()), intMap)
        assertEquals(1, v8.objectReferenceCount - refCountStart)
    }

    @Test(expected = V8EncodingException::class)
    fun longKeyedMapToV8ThrowsException() {
        val longMap = mapOf(1L to "1", 2L to "2", 3L to "3")
        val refCountStart = v8.objectReferenceCount
        k2V8.toV8(MapSerializer(Long.serializer(), String.serializer()), longMap)
        assertEquals(1, v8.objectReferenceCount - refCountStart)
    }

    @Test(expected = V8EncodingException::class)
    fun doubleKeyedMapToV8ThrowsException() {
        val doubleMap = mapOf(1.0 to "1", 2.0 to "2", 3.0 to "3")
        val refCountStart = v8.objectReferenceCount
        k2V8.toV8(MapSerializer(Double.serializer(), String.serializer()), doubleMap)
        assertEquals(1, v8.objectReferenceCount - refCountStart)
    }

    @Test(expected = V8EncodingException::class)
    fun floatKeyedMapToV8ThrowsException() {
        val floatMap = mapOf(1f to "1", 2f to "2", 3f to "3")
        val refCountStart = v8.objectReferenceCount
        k2V8.toV8(MapSerializer(Float.serializer(), String.serializer()), floatMap)
        assertEquals(1, v8.objectReferenceCount - refCountStart)
    }

    @Test
    fun undefinedValueIsSerialized() {
        val jsonObject = v8.executeScript("var result = {\"value\": \"foo\", \"nestedList\": undefined, \"nestedMap\": undefined}; result")
        val refCountStart = v8.objectReferenceCount
        val v8Object = k2V8.fromV8(NullableNestedObject.serializer(), jsonObject as V8Object)
        assertEquals(0, v8.objectReferenceCount - refCountStart)
        v8Object.value.shouldBe("foo")
        v8Object.nestedList?.shouldBeEmpty()
        v8Object.nestedMap?.shouldBe(mapOf())
    }

    // endregion

    // region helpers

    private fun getSupportedTypes(
            chars: List<Char>,
            longs: List<Long>,
            strings: List<String>,
            enums: List<Enum>,
            stringMap: Map<String, String>
    ) =
            SupportedTypes(
                    byte = chars.random().toByte(),
                    nullByte = null,
                    nonNullByte = chars.random().toByte(),
                    short = chars.random().toShort(),
                    nullShort = null,
                    nonNullShort = chars.random().toShort(),
                    char = chars.random(),
                    nullChar = null,
                    nonNullChar = chars.random(),
                    int = longs.random().toInt(),
                    nullInt = null,
                    nonNullInt = longs.random().toInt(),
                    long = longs.random(),
                    nullLong = null,
                    nonNullLong = longs.random(),
                    float = longs.random().toFloat(),
                    nullFloat = null,
                    nonNullFloat = longs.random().toFloat(),
                    double = longs.random().toDouble(),
                    nullDouble = null,
                    nonNullDouble = longs.random().toDouble(),
                    string = strings.random(),
                    nullString = null,
                    nonNullString = strings.random(),
                    boolean = true,
                    nullBoolean = null,
                    nonNullBoolean = true,
                    enum = enums.random(),
                    nullEnum = null,
                    unit = Unit,
                    nestedObject = NestedObject(strings.random()),
                    nullNestedObject = null,
                    nonNullNestedObject = NestedObject(strings.random()),
                    doubleNestedObject = DoubleNestedObject(NestedObject(strings.random())),
                    byteList = chars.map { it.toByte() },
                    shortList = chars.map { it.toShort() },
                    charList = chars,
                    intList = longs.map { it.toInt() },
                    longList = longs,
                    floatList = longs.map { it.toFloat() },
                    doubleList = longs.map { it.toDouble() },
                    stringList = strings,
                    booleanList = listOf(true, false, true),
                    enumList = enums,
                    nestedObjectList = strings.map { NestedObject(it) },
                    stringMap = stringMap,
                    enumMap = mapOf(
                            enums.random() to strings.random(),
                            enums.random() to strings.random(),
                            enums.random() to strings.random()
                    )
            )

    // endregion
}
