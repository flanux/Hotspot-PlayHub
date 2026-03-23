package com.hotspotplayhub.engine

import java.net.Socket

/**
 * Represents a connected player in the session
 */
data class Player(
    val id: Byte,
    val name: String,
    val deviceName: String,
    val socket: Socket? = null,
    var isReady: Boolean = false
) {
    override fun toString(): String {
        return "$name ($deviceName)"
    }
}
