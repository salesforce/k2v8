/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class K2V8ExceptionTest: K2V8TestBase() {

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
        } catch ( ex: V8DecodingException) {
            Assert.assertEquals(0, v8.objectReferenceCount - refCountStart)
            throw ex
        }
    }
}