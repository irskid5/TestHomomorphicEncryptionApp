cmake_minimum_required(VERSION 3.4.1)

add_library(SealJavaWrapper
            SHARED
            src/main/cpp/SealJavaWrapper.cpp)

include_directories(../external/SEAL/native/src)

add_library(seal STATIC "")
add_subdirectory(../external/SEAL/native/src/seal build)

target_link_libraries(SealJavaWrapper
                      seal)