/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8ResultUndefined
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class K2V8ExceptionTest : K2V8TestBase() {

    @Serializable
    class MapData(val data: Map<Int, String>)

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test(expected = V8DecodingException::class)
    fun enumListFromV8WithInvalidItem() = v8.scope {
        val array = Enum.values()
                .map { it.toString() }
                .toMutableList()
                .apply { add("VALUE_invalid") }
                .toV8Array(v8)

        val refCountStart = v8.objectReferenceCount
        try {
            k2V8.fromV8(ListSerializer(Enum.serializer()), array)
        } catch (ex: V8DecodingException) {
            Assert.assertEquals(0, v8.objectReferenceCount - refCountStart)
            throw ex
        }
    }


    // Verify decoding a class missing required field and no memory leak
    // kotlinx.serialization.UnknownFieldException is thrown. It's internal, so use Exception
    @Test(expected = Exception::class)
    fun objFromV8MissingNoNullableField() = v8.scope {
        val v8Object = V8Object(v8)

        val refCountStart = v8.objectReferenceCount
        try {
            k2V8.fromV8(SealedClass.ClassOne.serializer(), v8Object)
        } catch (ex: Exception) {
            Assert.assertEquals(0, v8.objectReferenceCount - refCountStart)
            throw ex
        }
    }

    /**
     * Give an integration to an object property which suppose to be object and
     * verify decoding fail and no memory leak
     */
    @Test(expected = V8ResultUndefined::class)
    fun objectFromV8WrongTypeOfField() = v8.scope {
        val v8Object = V8Object(v8).apply {
            add("nestedObject", 123)
            add("100", "100")
        }

        val refCountStart = v8.objectReferenceCount
        try {
            k2V8.fromV8(DoubleNestedObject.serializer(), v8Object)
        } catch (ex: V8ResultUndefined) {
            Assert.assertEquals(0, v8.objectReferenceCount - refCountStart)
            throw ex
        }
    }

    /**
     * Currently toV8 doesn't support map<Int, *>. so exception raised.
     * this test verify no memory leak even with exception
     */
    @Test(expected = V8EncodingException::class)
    fun objectToV8() = v8.scope {
        val refCountStart = v8.objectReferenceCount
        try {
            k2V8.toV8(MapData.serializer(), MapData(mapOf(100 to "100")))
        } catch (ex: V8EncodingException) {
            Assert.assertEquals(0, v8.objectReferenceCount - refCountStart)
            throw ex
        }
    }
}