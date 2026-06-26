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
    const val GET_SYSTEM_DIRECTORY = 9
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
    const val GET_INPUT_BITMASKS = 51
}

internal object PixelFormatC {
    const val ORGB1555 = 0
    const val XRGB8888 = 1
    const val RGB565 = 2
}
