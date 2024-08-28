kSteam
---

kSteam is a **JVM/Android Kotlin library** which allows you to connect to the Valve's [Steam](https://store.steampowered.com/) network.

Its usage is mostly based on **Kotlin Coroutines** and **states** to better suit for modern application development.

> This library is in very early state, so expect bugs and incomplete features. Also, releases are mostly not binary compatible between each other due to rapid development process.

> Please note that this library is **UNOFFICIAL** and not endorsed, sponsored, allowed, developed by Valve Corporation or related to it. Don't report bugs to them!
---

### Usage

Refer to the [examples document](README_Examples.md) for more information how to use kSteam for various needs.

You can also check out [Cobalt](https://github.com/iTaysonLab/Jetisteam/) - an Android replacement for the official Steam client that uses kSteam.

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

Using Android target with Jetpack Compose provides better support for using kSteam models directly in the UI with `@Immutable`/`@Stable` annotations.

### Features
- Access to the Steam network by using the modern WebSocket approach
- Stable and async-first architecture with proper documentation
- Providing Flows and suspending API for reactive UIs without any callback hell

### Goals
- Provide an easy-to-use library for accessing the Steam network on JVM/Android
- Make UI development easier by providing state-based approach without taking care of Protocol Buffers
- Manage high performance and low memory/storage footprint by using well-tested modern technology such as Wire (for protobufs) and Ktor (for networking)
- Provide full Steam Guard and new auth flow support
- Removing the gap between WebAPI and Steam3 messages
- Actively cache data for minimizing network usage and portability

### Modules
- `core`: main kSteam core, especially a small client that can connect to the Steam network and handle API/Authorization requests.
- `core-persistence`: implementation for kSteam persistence subsystem, allowing for saving user credentials and machine tokens.
- `extension-client`: full-blown client based on kSteam core that provides user-friendly access to Steam API with active caching. **Relies heavily on Realm Database.**
- `kotlinx-vdf`: KotlinX Serialization adapter for Valve Data Format, useful for PICS/Library parsing.
- `proto-common`: common Steam protobufs used in several kSteam modules.

### Which module set should I use?

If you are planning to use kSteam only for basic Steam communication, consider using `core` and `core-persistance` (optional) modules. They provide authorization, credential management (optional) and raw Steam API connection (by using protobufs or binary messages).

However, if you are going to create a GUI client, consider including the `extension-client` module as well (if your target platform is supported by the Realm Database). It provides a lot of useful Steam API mappings paired with automatic Kotlin Flow support for dynamic UI.

The `kotlinx-vdf` module is already provided with the modules above, but you can import it separately in case of not requiring to use any of kSteam features.

### Credits
- [SteamKit](https://github.com/SteamRE/SteamKit/) and [JavaSteam](https://github.com/Longi94/JavaSteam/) - base for understanding how Steam3 network works
- [SteamDB protobuf repository](https://github.com/SteamDatabase/Protobufs/) is used in modules
- [Ktor](https://github.com/ktorio/ktor) is used as a networking library
- [Wire](https://github.com/square/wire) is used for protobuf decoding/encoding
- [Okio](https://github.com/square/okio) is used for cross-platform filesystem access, VDF parsing and byte manipulation
- [Immutable Collections Library for Kotlin](https://github.com/Kotlin/kotlinx.collections.immutable)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)
- [cache4k](https://github.com/ReactiveCircus/cache4k) for synchronized in-memory cache