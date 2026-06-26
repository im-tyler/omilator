package com.omilator.core.libretro.jvm

import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout

internal object LibretroLayouts {

    val cString: ValueLayout = ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_BYTE)

    val systemInfo: MemoryLayout = MemoryLayout.structLayout(
        cString.withName("library_name"),
        cString.withName("library_version"),
        cString.withName("valid_extensions"),
        ValueLayout.JAVA_BYTE.withName("need_fullpath"),
        ValueLayout.JAVA_BYTE.withName("block_extract"),
        MemoryLayout.paddingLayout(6),
    )

    val gameInfo: MemoryLayout = MemoryLayout.structLayout(
        cString.withName("path"),
        ValueLayout.ADDRESS.withName("data"),
        ValueLayout.JAVA_LONG.withName("size"),
        cString.withName("meta"),
    )

    val gameGeometry: MemoryLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("base_width"),
        ValueLayout.JAVA_INT.withName("base_height"),
        ValueLayout.JAVA_INT.withName("max_width"),
        ValueLayout.JAVA_INT.withName("max_height"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.JAVA_FLOAT.withName("aspect_ratio"),
    )

    val systemTiming: MemoryLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_DOUBLE.withName("fps"),
        ValueLayout.JAVA_DOUBLE.withName("sample_rate"),
    )

    val systemAvInfo: MemoryLayout = MemoryLayout.structLayout(
        gameGeometry.withName("geometry"),
        systemTiming.withName("timing"),
    )
}

internal object RetroEnv {
    const val GET_API_VERSION = 0
    const val GET_LOG_INTERFACE = 6
    const val GET_SYSTEM_DIRECTORY = 9
    const val GET_SAVE_DIRECTORY = 31
    const val SET_PIXEL_FORMAT = 10
    const val SET_INPUT_DESCRIPTORS = 11
    const val GET_VARIABLE = 15
    const val SET_VARIABLES = 12
    const val GET_VARIABLE_UPDATE = 17
    const val SET_FRAME_TIME_CALLBACK = 16
    const val SET_AUDIO_CALLBACK = 13
    const val GET_LIBRETRO_PATH = 19
    const val GET_CORE_OPTIONS_VERSION = 52
    const val SET_CORE_OPTIONS = 53
    const val GET_CORE_OPTIONS_UPDATE = 54
    const val SET_CONTROLLER_INFO = 35
    const val GET_INPUT_INTERFACE = 23
    const val GET_INPUT_BITMASKS = 51
    const val SET_HW_RENDER = 14
    const val GET_PREFERRED_HW_RENDER = 36
    const val GET_AUDIO_VIDEO_ENABLE = 69
    const val GET_RUMBLE_INTERFACE = 27
    const val GET_CAMERA_DRIVER = 38
    const val GET_TARGET_REFRESH_RATE = 59
    const val SET_SUPPORT_ACHIEVEMENTS = 38 // alias — both used as 38 by different cores
}

internal object PixelFormatC {
    const val ORGB1555 = 0
    const val XRGB8888 = 1
    const val RGB565 = 2
}
