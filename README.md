kSteam
---

kSteam is a **JVM/Android Kotlin library** which allows you to connect to the Valve's [Steam](https://store.steampowered.com/) network.

Its usage is mostly based on **Kotlin Coroutines** and **states** to better suit for modern application development.

> [!CAUTION]
> This library is in very early state, so expect bugs and incomplete features. Also, releases are mostly not binary compatible between each other due to rapid development process.

> [!IMPORTANT]
> Please note that this library is **UNOFFICIAL** and not endorsed, sponsored, allowed, developed by Valve Corporation or related to it. Don't report bugs to them!
---

### Features
- Access to the Steam network by using the modern WebSocket approach
- Stable and async-first architecture with proper documentation
- Providing Flows and suspending API for reactive UIs without any "callback hell"

### Goals
- Provide an easy-to-use library for accessing the Steam network on JVM/Android
- Make UI development easier by providing state-based approach without taking care of Protocol Buffers
- Manage high performance and low memory/storage footprint by using well-tested modern technology such as Wire (for protobufs) and Ktor (for networking)
- Provide full Steam Guard and new auth flow support
- Removing the gap between WebAPI and Steam3 messages
- Actively cache data for minimizing network usage and portability

### Usage

Refer to the [examples document](README_Examples.md) for more information how to use kSteam for various needs.

You can also check out [Cobalt](https://github.com/iTaysonLab/Jetisteam/) - an Android replacement for the official Steam client that uses kSteam.

### Building

> Note: if you want to try experimental iOS K/Native support, set `ALLOW_APPLE_PLATFORMS` to `true` in `build-extensions/src/main/kotlin/CoreMppSetup.kt` before building.

Because kSteam is still in a very early stage, you will need to compile it before using. However, the process is streamlined:

1. Clone the repository.
2. Run `./gradlew publishToMavenLocal`. 
3. Every kSteam module will now be available on local Maven repository.

Tested on **IntelliJ IDEA 2024.3.1.1** with **JDK 17**.

### Which module set should I use?

If you are planning to use kSteam only for basic Steam communication, consider using `core` and `core-persistance` (optional) modules. They provide authorization, credential management (optional) and raw Steam API connection (by using protobufs or binary messages).

However, if you are going to create a GUI client, consider including the `extension-client` module as well (if your target platform is supported by the AndroidX Embedded SQLite). It provides a lot of useful Steam API mappings paired with automatic Kotlin Flow support for dynamic UI.

The `kotlinx-vdf` module is already provided with the modules above, but you can import it separately in case of not requiring to use any of kSteam features.

### Modules

```kotlin
dependencies {
    // Main kSteam core: a small client that connects to the Steam network. Also handles API and authorization requests.
    implementation("bruhcollective.itaysonlab.ksteam:core:$VERSION")
    
    // Persistence module that is used to save authorization data between sessions
    implementation("bruhcollective.itaysonlab.ksteam:core-persistence:$VERSION")

    // Steam Protobuf definitions compiled to Wire Kotlin. Not required to include if you include the core.
    implementation("bruhcollective.itaysonlab.ksteam:proto-common:$VERSION")

    // A full-blown client based on kSteam core that provides user-friendly access to Steam API with active caching.
    // Requires a platform with AndroidX Embedded SQLite driver support, which will be included as well.
    implementation("bruhcollective.itaysonlab.ksteam:extension-client:$VERSION")

    // Experimental kotlinx.serialization VDF module
    implementation("bruhcollective.itaysonlab:kotlinx-vdf-android:$VERSION")
}
```

### Platform Support

Supported:
- jvm
- android

Experimental:
- iosX64, iosArm64, iosSimulatorArm64
- macosX64, macosArm64

Unsupported:
- androidNativeX64, androidNativeX86
- linuxX64, linuxArm64
- watchosDeviceArm64, watchosSimulatorArm64, watchosX64, watchosArm32, watchosArm64
- tvosX64, tvosSimulatorArm64, tvosArm64
- mingwX64

### Jetpack Compose

If you are using Jetpack Compose, you would probably want to mark kSteam models as stable. 

To do that, add the following lines to your Compose module's build script:
```kotlin
composeCompiler {
    stabilityConfigurationFile = layout.projectDirectory.file("stability.conf")
}
```
and add the following to the file `stability.conf` _(any file name can be used)_
```text
bruhcollective.itaysonlab.ksteam.models.**
```

This will significantly reduce the number of recompositions if you are directly using kSteam models in composables.

### Credits
- [SteamKit](https://github.com/SteamRE/SteamKit/) and [JavaSteam](https://github.com/Longi94/JavaSteam/) - base for understanding how Steam3 network works
- [SteamDB protobuf repository](https://github.com/SteamDatabase/Protobufs/) is used in modules
- [Ktor](https://github.com/ktorio/ktor) is used as a networking library
- [Wire](https://github.com/square/wire) is used for protobuf decoding/encoding
- [Okio](https://github.com/square/okio) is used for cross-platform filesystem access, VDF parsing and byte manipulation
- [Immutable Collections Library for Kotlin](https://github.com/Kotlin/kotlinx.collections.immutable)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)
- [cache4k](https://github.com/ReactiveCircus/cache4k) for synchronized in-memory cache