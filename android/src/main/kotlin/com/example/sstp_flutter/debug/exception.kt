package com.example.sstp_flutter.debug


internal class ParsingDataUnitException : Exception("Failed to parse data unit")

internal fun assertAlways(value: Boolean) {
    if (!value) {
        throw AssertionError()
    }
}
