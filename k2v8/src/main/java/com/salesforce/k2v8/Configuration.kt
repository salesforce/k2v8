/*
 * Copyright (c) 2020, Salesforce.com, inc.
 *  All rights reserved.
 *  SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.k2v8

import com.eclipsesource.v8.V8

/**
 * Configuration class to configure a [K2V8] instance. The [runtime] is the [V8] instance that
 * will be used to serialize/deserialize objects to/from.
 */
class Configuration(val runtime: V8)
