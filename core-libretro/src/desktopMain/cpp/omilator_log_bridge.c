/*
 * Omilator C log bridge.
 *
 * Provides a stable ABI function pointer for the retro_log_callback
 * struct. The libretro log signature is variadic:
 *
 *   void (*)(enum retro_log_level level, const char *fmt, ...)
 *
 * JDK 21's Foreign Function & Memory API cannot reliably upcall into
 * Kotlin for variadic C functions on macOS arm64 — the generated stub
 * triggers BUS_ADRALN when the core passes varargs. PPSSPP, Dolphin,
 * and others call log() heavily during retro_init.
 *
 * This file is compiled to a tiny .dylib via clang. We dlopen it at
 * runtime and pass the function pointer to the core. No FFM upcall
 * is on the hot path.
 */

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdbool.h>

/* Matches retro_log_level in libretro.h */
enum omilator_log_level {
    OMILATOR_LOG_ERROR = 0,
    OMILATOR_LOG_WARN  = 1,
    OMILATOR_LOG_INFO  = 2,
    OMILATOR_LOG_DEBUG = 3,
};

static const char *level_name(int level) {
    switch (level) {
        case OMILATOR_LOG_ERROR: return "ERROR";
        case OMILATOR_LOG_WARN:  return "WARN ";
        case OMILATOR_LOG_INFO:  return "INFO ";
        case OMILATOR_LOG_DEBUG: return "DEBUG";
        default:                  return "L?   ";
    }
}

/* retro_log_printf_t signature: void(int level, const char *fmt, ...) */
void omilator_log_callback(int level, const char *fmt, ...) {
    if (fmt == NULL) return;

    va_list args;
    va_start(args, fmt);

    fputs("[core/log] ", stderr);
    fputs(level_name(level), stderr);
    fputc(' ', stderr);
    vfprintf(stderr, fmt, args);

    size_t len = strlen(fmt);
    if (len == 0 || fmt[len - 1] != '\n') {
        fputc('\n', stderr);
    }

    fflush(stderr);
    va_end(args);
}

/*
 * Stable no-op stubs for other callback interfaces that PPSSPP and
 * similar cores query. Same pattern: variadic-safe C ABI, no FFM
 * upcall on the hot path.
 */

/* retro_set_rumble_state_t: bool (unsigned port, unsigned effect, uint16_t strength) */
bool omilator_rumble_callback(unsigned port, unsigned effect, unsigned short strength) {
    return true;
}
