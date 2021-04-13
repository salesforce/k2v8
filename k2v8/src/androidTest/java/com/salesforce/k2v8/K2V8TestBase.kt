/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8
import kotlinx.serialization.Serializable

open class K2V8TestBase {
    @Serializable
    @Suppress("unused")
    enum class Enum {
        VALUE_1,
        VALUE_2,
        VALUE_3
    }

    @Serializable
    sealed class SealedClass {

        @Serializable
        data class ClassOne(val someString: String) : SealedClass()

        @Serializable
        data class ClassTwo(val someInt: Int) : SealedClass()
    }

    @Serializable
    data class SupportedTypes(
            val byte: Byte,
            val nullByte: Byte?,
            val nonNullByte: Byte?,
            val short: Short,
            val nullShort: Short?,
            val nonNullShort: Short?,
            val char: Char,
            val nullChar: Char?,
            val nonNullChar: Char?,
            val int: Int,
            val nullInt: Int?,
            val nonNullInt: Int?,
            val long: Long,
            val nullLong: Long?,
            val nonNullLong: Long?,
            val double: Double,
            val nullDouble: Double?,
            val nonNullDouble: Double?,
            val float: Float,
            val nullFloat: Float?,
            val nonNullFloat: Float?,
            val string: String,
            val nullString: String?,
            val nonNullString: String?,
            val boolean: Boolean,
            val nullBoolean: Boolean?,
            val nonNullBoolean: Boolean?,
            val enum: Enum,
            val nullEnum: Enum?,
            val unit: Unit,
            val nestedObject: NestedObject,
            val nullNestedObject: NestedObject?,
            val nonNullNestedObject: NestedObject?,
            val doubleNestedObject: DoubleNestedObject,
            val byteList: List<Byte>,
            val shortList: List<Short>,
            val charList: List<Char>,
            val intList: List<Int>,
            val longList: List<Long>,
            val floatList: List<Float>,
            val doubleList: List<Double>,
            val stringList: List<String>,
            val booleanList: List<Boolean>,
            val enumList: List<Enum>,
            val nestedObjectList: List<NestedObject>,
            val stringMap: Map<String, String>,
            val enumMap: Map<Enum, String>
    )

    @Serializable
    data class NestedObject(
            val value: String
    )

    @Serializable
    data class DoubleNestedObject(
            val nestedObject: NestedObject
    )

    @Serializable
    data class NullableNestedObject(
            val value: String,
            val nestedMap: Map<String, String>? = null,
            val nestedList: List<NestedObject>? = null
    )

    protected lateinit var v8: V8
    protected lateinit var k2V8: K2V8

    open fun setUp() {
        v8 = V8.createV8Runtime()
        k2V8 = K2V8(Configuration(v8))
    }
}