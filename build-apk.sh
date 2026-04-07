#!/bin/bash

# StreamFlix APK Builder Script
# Works on Termux, Linux, and macOS

set -e

echo "======================================"
echo "  StreamFlix APK Builder"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    print_error "Please run this script from the StreamFlix project root directory"
    exit 1
fi

# Detect OS
OS="unknown"
if [[ "$OSTYPE" == "linux-android"* ]]; then
    OS="termux"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
fi

print_info "Detected OS: $OS"

# Check for Java
if ! command -v java &> /dev/null; then
    print_error "Java not found! Please install Java 17"
    
    if [ "$OS" == "termux" ]; then
        echo "Run: pkg install openjdk-17"
    elif [ "$OS" == "linux" ]; then
        echo "Run: sudo apt install openjdk-17-jdk"
    elif [ "$OS" == "macos" ]; then
        echo "Run: brew install openjdk@17"
    fi
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
print_info "Java version: $JAVA_VERSION"

# Make gradlew executable
if [ ! -x "./gradlew" ]; then
    print_info "Making gradlew executable..."
    chmod +x gradlew
fi

# Clean previous builds
print_info "Cleaning previous builds..."
./gradlew clean

# Build APK
print_info "Building Debug APK..."
./gradlew assembleDebug

# Check if build succeeded
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    print_info "✅ Build successful!"
    echo ""
    print_info "APK location:"
    echo "  app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # Copy to accessible location on Termux
    if [ "$OS" == "termux" ]; then
        cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/StreamFlix-debug.apk
        print_info "APK also copied to: /sdcard/Download/StreamFlix-debug.apk"
    fi
    
    # Show APK size
    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    print_info "APK size: $APK_SIZE"
else
    print_error "Build failed! APK not found."
    exit 1
fi

echo ""
echo "======================================"
echo "  Build Complete!"
echo "======================================"
