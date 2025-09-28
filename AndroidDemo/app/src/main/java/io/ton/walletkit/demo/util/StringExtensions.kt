package io.ton.walletkit.demo.util

fun String.abbreviated(length: Int = 6): String {
    if (this.length <= length * 2) return this
    val prefix = take(length)
    val suffix = takeLast(length)
    return "$prefix…$suffix"
}
