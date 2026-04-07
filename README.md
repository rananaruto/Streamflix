# StreamFlix - Android Streaming App

A full-featured Android streaming application with Liquid Glass design, extension system, and ExoPlayer integration.

## Features

### UI/UX
- **Liquid Glass Design**: Semi-transparent cards, frosted blur backgrounds, smooth animations
- **Home Screen**: Dynamic movie/series poster grid with sections
- **Categories Screen**: Browse content by category with beautiful gradients
- **Search Screen**: Real-time search with debounced queries
- **Player Screen**: Full-featured video player with ExoPlayer
- **Profile & Settings**: User statistics and app preferences

### Core Features
- **Extension System**: Dynamic loading from GitHub or local extensions (like Cloudstream)
- **Web Scraping**: Jsoup-based content extraction
- **Video Streaming**: Support for m3u8, mp4, and DASH formats
- **Favorites**: Save and manage favorite content
- **Watch History**: Track viewing progress
- **Pull-to-Refresh**: In all list screens
- **Dark/Light Mode**: Full theme support

### Extension API
Each extension implements:
- `mainPage()`: Returns home page sections
- `search(query)`: Search functionality
- `loadMovie(url)`: Load movie/series details
- `loadLinks(url)`: Get streaming links

## Project Structure

```
app/src/main/
├── java/com/streamflix/
│   ├── data/
│   │   ├── local/          # Room database, DAOs
│   │   ├── model/          # Data models
│   │   └── repository/     # Repositories
│   ├── di/                 # Koin dependency injection
│   ├── extension/          # Extension system
│   │   ├── model/          # Extension API models
│   │   └── samples/        # Sample extensions
│   ├── ui/
│   │   ├── home/           # Home screen
│   │   ├── categories/     # Categories screen
│   │   ├── search/         # Search screen
│   │   ├── player/         # Video player
│   │   ├── favorites/      # Favorites screen
│   │   ├── profile/        # Profile screen
│   │   ├── settings/       # Settings screen
│   │   ├── extensions/     # Extension manager
│   │   └── history/        # Watch history
│   └── viewmodel/          # ViewModels
├── res/                    # Layouts, drawables, values
└── assets/extensions/      # Built-in extensions
```

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Koin
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Database**: Room
- **Video Player**: ExoPlayer (Media3)
- **Web Scraping**: Jsoup
- **UI**: Material Design 3

## Build Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34
- Minimum API 21 (Android 5.0)

### Building with Android Studio

1. **Open the project**:
   ```
   File -> Open -> Select StreamFlix folder
   ```

2. **Sync Gradle**:
   - Click "Sync Now" in the notification bar
   - Or use: `Tools -> File -> Sync Project with Gradle Files`

3. **Build the project**:
   ```
   Build -> Make Project (Ctrl+F9)
   ```

4. **Run on device/emulator**:
   ```
   Run -> Run 'app' (Shift+F10)
   ```

### Building with Mobile IDE (Android Code Studio / Acode)

1. **Install required tools**:
   ```bash
   # Install Android SDK Command Line Tools
   pkg install android-sdk
   ```

2. **Set environment variables**:
   ```bash
   export ANDROID_HOME=$HOME/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

3. **Build APK**:
   ```bash
   cd StreamFlix
   ./gradlew assembleDebug
   ```

4. **Find APK**:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Gradle Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK to connected device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## Extension Development

### Extension Manifest Format

```json
{
  "id": "sample.movies.extension",
  "name": "Sample Movies",
  "version": "1.0.0",
  "author": "Your Name",
  "description": "Extension description",
  "language": "en",
  "icon": "https://example.com/icon.png",
  "categories": ["Movies", "TV Shows"],
  "baseUrl": "https://example-streaming-site.com",
  "apiVersion": 1
}
```

### Extension Implementation

See `app/src/main/java/com/streamflix/extension/samples/SampleExtension.kt` for a complete example.

Key methods to implement:
- `getMainPage()`: Return home page sections
- `search(query, page)`: Search functionality
- `loadMovie(url)`: Load movie/series details
- `loadLinks(url)`: Get streaming links

### Installing Extensions

1. **From GitHub**:
   - Go to Profile -> Extensions
   - Tap "+" button
   - Enter GitHub repository URL
   - Tap Install

2. **From Local File**:
   - Place extension JSON in `/sdcard/Download/`
   - Go to Profile -> Extensions
   - Tap "+" -> "From Local File"
   - Select the extension file

## Configuration

### App Settings

Settings are stored in DataStore and include:
- Theme mode (Light/Dark/System)
- Liquid Glass effect toggle
- Auto-play next episode
- Default video quality
- Subtitle language preference

### Network Configuration

Network security config allows cleartext traffic for streaming sources:
- `res/xml/network_security_config.xml`

## Troubleshooting

### Build Issues

1. **Gradle sync failed**:
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

2. **Out of memory**:
   Add to `gradle.properties`:
   ```
   org.gradle.jvmargs=-Xmx4096m
   ```

3. **Dependency conflicts**:
   Check `build.gradle.kts` for version compatibility

### Runtime Issues

1. **Video not playing**:
   - Check extension is properly installed
   - Verify video URL is accessible
   - Check network security config

2. **Extensions not loading**:
   - Verify manifest format
   - Check baseUrl is correct
   - Enable extension in Extension Manager

## License

This project is for educational purposes. Respect content creators' rights and use responsibly.

## Credits

- ExoPlayer by Google
- Material Design Components
- Jsoup by Jonathan Hedley
- Coil by Coil Team
- Koin by InsertKoin

## Version History

- **1.0.0-LiquidGlass** (Current)
  - Initial release
  - Liquid Glass UI design
  - Extension system
  - ExoPlayer integration
  - Favorites and watch history
