/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.V8Value
import com.eclipsesource.v8.utils.MemoryManager
import com.eclipsesource.v8.utils.V8ObjectUtils

/**
 * Creates a scope around the function [body] releasing any objects after the [body] is invoked.
 */
inline fun V8.scope(body: () -> Unit) {
    val scope = MemoryManager(this)
    try {
        body()
    } finally {
        scope.release()
    }
}

/**
 * Creates a memory scope around the function [body].
 * After the [body] is invoked, any V8Value objects created in the body will be released
 * except the V8Value object is returned to caller for consumption.
 */
inline fun <T> V8.scopeWithResult(body: () -> T): T {
    val scope = MemoryManager(this)
    try {
        return body().apply {
            if (this is V8Value) {
                scope.persist(this)
            }
        }
    } finally {
        scope.release()
    }
}

/**
 * Converts a [V8Array] from a [List] typed [T].
 */
fun <T> List<T>.toV8Array(v8: V8): V8Array {
    return V8ObjectUtils.toV8Array(v8, this)
}

/**
 * Converts a [V8Array] from a [Map] typed [String, String].
 */
fun <V> Map<String, V>.toV8Object(v8: V8): V8Object {
    return V8ObjectUtils.toV8Object(v8, this)
}
