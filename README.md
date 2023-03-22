kSteam
---

kSteam is a **JVM/Android Kotlin library** which allows you to connect to the Steam network.

It's usage is mostly based on **Kotlin Coroutines** and **states** to better suit for modern application development.

> This library is in very early state, so expect bugs and incomplete features.
> 
> Please note that this library is **UNOFFICIAL** and not endorsed, sponsored, allowed, developed by Valve Corporation or related to it. Don't report bugs to them!
---

### Usage

```kotlin
// define a kSteam instance, this should be done once
// for some reasons you might launch several kSteam instances - this behavior is not tested
val steamClient = kSteam {
    // location where kSteam will store it's data like user accounts
    rootFolder = [File]
    
    // device info shown when other users will manage sessions or approve this one
    deviceInfo = DeviceInformation(
        osType = EOSType.k_eAndroidUnknown, // current OS
        gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_Phone, // device type
        platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient, // don't change this
        deviceName = "kSteam device" // session name
    )
    
    // install the extension-core extension to access basic Steam functions (profiles, news, etc)
    install(Core)
    
    // if you want, install extension-guard for Steam Guard support
    install(Guard) {
        uuid = "" // - specify your device UUID
    }
    
    // if you want, install extension-pics for more reliable owned games metadata and library management
    install(Pics) {
        database = YourKvDatabase() // - specify your key-value database implementation
    }
    
    // include any other extensions
}

// if needed, you can use a built-in dumper to analyze Steam Network packets
// they will be saved in rootFolder/dumps/%timestamp%/
steamClient.dumperMode = PacketDumper.DumpMode.Full

// connect to the Steam network
steamClient.start()

// kSteam saves user accounts in rootFolder and tries to sign in automatically after start()
if (steamClient.account.hasSavedDataForAtLeastOneAccount()) {
    steamClient.account.awaitSignIn()
} else {
    val signResult = steamClient.account.signIn(
        username = "username",
        password = "password",
        rememberSession = false // - "true" will save the account so on the next launch, you could use hasSavedDataForAtLeastOneAccount()
    )
    
    if (signResult is AuthorizationResult.Success) {
        // 2FA, use steamClient.account.clientAuthState Flow to manage it
        // TODO: write a sample
    } else if (signResult is AuthorizationResult.InvalidPassword) {
        // your code
    }
}

// now you are connected to the Steam network, do what you want...
// ...like subscribing to player's state...
steamClient.persona.currentPersona.onEach { persona ->
    // update the UI
}.launchIn(scope)

// ...or requesting someone's profile equipment
val kSteamDevEquipment = steamClient.profile.getEquipment(SteamId(76561198176883618_u))
val animatedAvatarUrl = kSteamDevEquipment.animatedAvatar?.movieMp4?.url // - mostly all of kSteam handlers provide "parsed" and Kotlin-friendly data structures 

// ...or even more...
```

### Features so far
- Access to the Steam network by using WebSocket approach
- Stable and async-first architecture with proper documentation
- Jetpack Compose friendly (using @Stable/@Immutable annotations so the compiler will recognize them - you can safely use most of kSteam classes inside Composables)
- Extension support which can greatly decrease final application size, while still readable and comfortable to use
- Providing Flows and suspending API for reactive UIs without any callback hell

### Goals
- Provide a easy-to-use library for accessing the Steam network on JVM/Android
- Make UI development easier by providing state-based approach without taking care of Protocol Buffers
- Manage high performance and low memory/storage footprint by using well-tested modern technology such as Wire (for protobufs) and Ktor (for networking)
- Provide full Steam Guard and new auth flow support
- Removing the gap between WebAPI and Steam3 messages
- Actively cache data for minimizing network usage and portability
- Aim for a Kotlin Multiplatform release somewhat in the future

### Credits
- [SteamKit](https://github.com/SteamRE/SteamKit/) and [JavaSteam](https://github.com/Longi94/JavaSteam/) - base for understanding how Steam3 network works
- [SteamDB protobuf repository](https://github.com/SteamDatabase/Protobufs/) is used in modules
- [Ktor](https://github.com/ktorio/ktor) is used as a networking library
- [Wire](https://github.com/square/wire) is used for protobuf decoding/encoding
- [Okio](https://github.com/square/okio) is used for cross-platform filesystem access, VDF parsing and byte manipulation