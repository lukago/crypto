cmake_minimum_required(VERSION 3.8)
project(elgamalc)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES main.cpp BigInt.cpp primes.h BigInt.h)
add_executable(elgamalc ${SOURCE_FILES})