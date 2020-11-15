/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.MemoryManager
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerialModule

/**
 * The main entry point for V8Object serialization.
 */
class K2V8(val configuration: Configuration, override val context: SerialModule = DefaultModule) :
    SerialFormat {

    /**
     * Serializes a [T] value to a [V8Object] using the [serializer] provided.
     */
    fun <T> toV8(serializer: SerializationStrategy<T>, value: T) =
        convertToV8Object(value, serializer)

    /**
     * Deserializes a [V8Object] value to a [T] using the [deserializer] provided.
     */
    fun <T> fromV8(deserializer: DeserializationStrategy<T>, value: V8Object): T {
        return convertFromV8Object(value, deserializer)
    }
}
