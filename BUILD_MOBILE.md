# Build StreamFlix APK on Mobile (Termux)

## Quick Start (5 minutes)

### Step 1: Install Termux
Download from F-Droid (recommended) or Play Store

### Step 2: One-Command Setup
Copy and paste this entire block into Termux:

```bash
# Update and install dependencies
pkg update -y && pkg upgrade -y
pkg install -y openjdk-17 gradle git

# Clone or navigate to project
cd ~
# If you have the project in Downloads:
cd /sdcard/Download/StreamFlix

# Or clone from GitHub (if uploaded):
# git clone https://github.com/yourusername/streamflix.git
# cd streamflix

# Build APK
chmod +x build-apk.sh
./build-apk.sh
```

### Step 3: Install APK
```bash
# The APK will be copied to Downloads
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/

# Now open your file manager and install from Downloads
```

---

## Manual Build (If script fails)

```bash
# 1. Navigate to project
cd /sdcard/Download/StreamFlix

# 2. Make gradlew executable
chmod +x gradlew

# 3. Clean and build
./gradlew clean
./gradlew assembleDebug

# 4. Get APK
ls -la app/build/outputs/apk/debug/
```

---

## Troubleshooting

### "Permission denied"
```bash
chmod +x gradlew
```

### "Java not found"
```bash
pkg install openjdk-17
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
```

### "Out of memory"
```bash
# Add to gradle.properties:
echo "org.gradle.jvmargs=-Xmx1024m" >> gradle.properties
```

### "Gradle daemon issues"
```bash
./gradlew --stop
./gradlew clean
./gradlew assembleDebug --no-daemon
```

---

## Build with Acode IDE

1. Open Acode app
2. Open StreamFlix folder
3. Open terminal in Acode
4. Run the build commands above

---

## Alternative: GitHub Actions (Easiest!)

1. Upload code to GitHub
2. Go to Actions tab
3. Click "Build StreamFlix APK"
4. Download APK from artifacts

No setup required! 🎉
