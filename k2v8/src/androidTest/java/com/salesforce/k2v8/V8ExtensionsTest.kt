/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class V8ExtensionsTest {
    private lateinit var v8: V8

    @Before
    fun setup() {
        v8 = V8.createV8Runtime()
    }

    @After
    fun cleanup() {
        v8.close()
    }

    @Test
    fun scopeTest() {
        val foo = V8Object(v8)
        assertEquals(1, v8.objectReferenceCount)
        foo.close()
        assertEquals(0, v8.objectReferenceCount)

        // verify the object is released once scope is done
        v8.scope {
            V8Object(v8)
        }
        assertEquals(0, v8.objectReferenceCount)
    }

    @Test
    fun scopeWithResult() = v8.scope {
        val foo = V8Object(v8)
        assertEquals(1, v8.objectReferenceCount)
        foo.close()
        assertEquals(0, v8.objectReferenceCount)

        // verify the object returned object still not release, others are.
        val result = v8.scopeWithResult {
            val barObj = V8Object(v8)
            V8Object(v8)
        }
        assertEquals(1, v8.objectReferenceCount)
    }
}