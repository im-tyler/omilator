package com.omilator.ui

import kotlin.concurrent.Volatile

/**
 * Process-wide slot for the most recent omilator://play/<path> URL.
 * iOSApp.swift's .onOpenURL writes here; RootViewController's
 * LaunchedEffect polls every 100ms and launches the player when a path
 * appears. Cleared on consume.
 *
 * We use a polled Volatile var instead of StateFlow because collectAsState
 * wasn't reliably triggering recomposition on iOS when the flow was updated
 * from Swift's onOpenURL (likely a dispatcher handoff issue).
 *
 * Path is the absolute filesystem path to the ROM (URL-decoded).
 */
@Volatile
var pendingPlayRom: String? = null

/** Called by iOSApp.swift's onOpenURL handler. */
fun setPendingPlayRom(path: String?) {
    pendingPlayRom = path
}
