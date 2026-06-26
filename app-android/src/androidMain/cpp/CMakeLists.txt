cmake_minimum_required(VERSION 3.22.1)
project(omilator_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(omilator_jni SHARED
    libretro_jni.cpp
)

target_include_directories(omilator_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(log-lib log)

target_link_libraries(omilator_jni
    ${log-lib}
    dl
)
