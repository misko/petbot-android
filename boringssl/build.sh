#!/bin/bash

#script by misko
export ANDROID_NDK=/Users/miskodzamba/StudioProjects/android-ndk-r12b

dist="dist"
build="build"


rm -rf ./$dist
mkdir ./$dist

archs="armeabi armeabi-v7a arm64-v8a x86 x86_64"


for arch in $archs; do 
	rm -rf ./$build

	mkdir ./$build
	mkdir ./$dist/$arch
	cd ./$build
	cmake -DANDROID_ABI=armeabi-v7a \
	      -DCMAKE_TOOLCHAIN_FILE=../third_party/android-cmake/android.toolchain.cmake \
	      -DANDROID_NATIVE_API_LEVEL=21 \
	      -GNinja ..
	ninja
	cd ..

	cp ./$build/ssl/libssl.a ./$dist/$arch
	cp ./$build/crypto/libcrypto.a ./$dist/$arch
done
