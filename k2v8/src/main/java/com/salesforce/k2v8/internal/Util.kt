package com.salesforce.k2v8.internal

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Adapted from [kotlinx.serialization.internal.cachedSerialNames]
 */
internal fun SerialDescriptor.cachedSerialNames(): Set<String> {
    val result = HashSet<String>(elementsCount)
    for (i in 0 until elementsCount) {
        result += getElementName(i)
    }
    return result
}