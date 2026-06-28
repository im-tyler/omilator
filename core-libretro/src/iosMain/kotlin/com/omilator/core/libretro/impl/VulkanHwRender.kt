@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.omilator.core.libretro.impl

import kotlinx.cinterop.*
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.dlclose
import platform.posix.RTLD_NOW

/**
 * Vulkan HW render bridge for iOS (via MoltenVK).
 *
 * Lifecycle:
 *  1. `prepare()` — dlopen MoltenVK. Cheap; no VkInstance yet.
 *  2. NativeCoreController installs our `get_proc_address` and
 *     `get_current_framebuffer` into retro_hw_render_callback (cmd 14).
 *  3. The core's `context_reset` is invoked next (by the core itself once
 *     SET_HW_RENDER returns true). It calls our get_proc_address to load
 *     `vkGetInstanceProcAddr`, then bootstraps the rest of Vulkan from there.
 *
 * Status: scaffolding only. The VkInstance + VkSurfaceKHR + VkSwapchainKHR
 * plumbing is NOT implemented. PSP/Dolphin cores will call
 * get_proc_address successfully but their vkCreateInstance call will
 * currently return VK_ERROR_INITIALIZATION_FAILED because we don't yet
 * provide VK_ICD_WSI / VK_KHR_portability_enumeration bits MoltenVK needs.
 *
 * What works today:
 *  - get_proc_address loads vkGetInstanceProcAddr + friends from MoltenVK
 *  - SET_HW_RENDER handler accepts VULKAN context_type and returns true
 *  - get_current_framebuffer returns 0 (placeholder)
 *
 * What's needed for actual rendering (future work):
 *  - vkCreateMetalSurfaceEXT wrapping a CAMetalLayer we own
 *  - VkSwapchainKHR creation + VkImage acquisition per frame
 *  - vkQueuePresentKHR integration with the Compose render loop
 *  - Thread-affinity: Vulkan command submission must happen on the same
 *    thread that called retro_run (the @ThreadLocal NativeCoreController).
 */
internal class VulkanHwRender {

    /** dlopen handle for MoltenVK.framework's binary — used by getProcAddress. */
    private var moltenVkHandle: CPointer<*>? = null

    /**
     * Initialise Metal + dlopen MoltenVK. Idempotent.
     * Returns false if MoltenVK can't be loaded — caller should reject
     * the SET_HW_RENDER request so the core can fall back to software.
     */
    fun prepare(): Boolean {
        if (moltenVkHandle == null) {
            // MoltenVK.framework is embedded in the app bundle by xcodegen
            // (see iosApp/project.yml: dependencies: - framework: ... MoltenVK.xcframework, embed: true).
            // iOS resolves "-framework MoltenVK" at link time so dlopen with
            // the install name should re-resolve to the bundled copy.
            moltenVkHandle = dlopen("MoltenVK.framework/MoltenVK", RTLD_NOW)
                ?: dlopen("libMoltenVK.dylib", RTLD_NOW)
        }
        return moltenVkHandle != null
    }

    /**
     * Returns a Vulkan function pointer by name from MoltenVK, or null.
     * The frontend installs this as retro_hw_render_callback.get_proc_address.
     * Cores call it to load `vkGetInstanceProcAddr` and from there bootstrap
     * the rest of the Vulkan API.
     */
    fun getProcAddress(sym: String): CPointer<*>? {
        val h = moltenVkHandle ?: return null
        return dlsym(h, sym)
    }

    /**
     * Returns the current framebuffer as an opaque uintptr_t.
     * For Vulkan, this is the VkImage handle the core should render into.
     * Currently returns 0 (placeholder) — actual rendering needs swapchain
     * plumbing first.
     */
    fun currentFramebuffer(): ULong = 0uL

    /**
     * Advance the swapchain — called by NativeCoreController after retro_run
     * returns so the core's rendering appears on screen. Currently a no-op.
     */
    fun presentFrame() {
        // TODO: vkQueuePresentKHR(swapchain.currentImage)
    }

    fun release() {
        moltenVkHandle?.let { dlclose(it) }
        moltenVkHandle = null
    }
}
