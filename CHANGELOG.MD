## r32 [WIP]

*Literally everything here is changed, more like a rewrite from scratch, leaving ~25% of original codebase*

- **[breaking change]** massive persistence rework, saved storage from earlier kSteam versions IS NOT COMPATIBLE (including Steam Guard)
- re-enable support for macOS/iOS compilation
- **[breaking change]** massive rework of extension architecture, simplifying everything to "core"-"implementation" model
- **[breaking change]** introduce `core-persistence` for managing saved user data
- `core/unifiedmessages`: provide a custom Wire client implementation for simplifying API requests
- **[breaking change]** `core/account`: now uses secure persistence
- `extension-client`: introduce Realm Mobile Database for persisting personas, collections, game metadata  
- `extension-client/persona`: optimize flows by using Realm queries
- **[breaking change]** `extension-client/pics`: key-value DB is not required anymore
- **[breaking change]** `extension-client/pics`: massive rework of querying, caching and storing to minimize extra requests
- `extension-client/guard`: now uses secure persistence

## r31

- **[breaking change]** Disable support for macOS/iOS compilation, effectively making kSteam a JVM/Android-only library
- **[breaking change]** RPC calls now use Wire's gRPC custom client implementation instead of manually passing request/response adapters. This also enables usage of Steam-defined services directly.
- `extension-pics`: support for 64-bit app IDs, making the client not crashing if providing folders with non-Steam applications 
- update dependencies
- streamline the version updating procedure by introducing version catalogs
- synchronized protobufs with the latest provided by Steam

---

## r30

---

- Matches Cobalt `1.0-prototype` build

## r29

---

- `extension-core/news`: implement `getUpcomingEvents` and `getUpcomingEvents`
- `extension-core/news`: extract friend activity to `UserNews` handler
- `extension-core/news`: rework `ActivityFeedEntry` to be more UI-compliant
- `extension-core/news`: other fixes and improvements
- `core/webapi`: reworked API wrappers to be more modern and Kotlin-friendly
- rewrite logging transport, now message value computation is done lazily (and is more likely to be removed by R8/Proguard)
- streamline build process
- provide `android` artifacts as well, with Compose stability bindings and logcat output

## r28

---

- **[breaking change]** redone `CMClientState` featuring new `AwaitingAuthorization` state (connected as a guest) and renamed `Logging` to `Authorizing` + `Idle` to `Offline`
- `core`: improved `CMClientState` exposing
- `core`: introduce `extractAccountIdFromSteam` and `toSteamId` extensions for `Long` and `Int`
- `core`: fixed rare Web API call crash
- `core/player`: make `getAchievementsProgress` and `getTopAchievements` public (proto API only for now)
- `extension-pics`: optimized database lists, fixing `CallbackSuspendableMap must be initialized first!` crash
- `extension-core/news`: finalized parameters for `getUserNews`, introduced `UserNewsFilterScenario`
- `extension-core/news`
- updated `kotlin` from `1.8.20` to `1.8.21`
- updated `kotlinx-coroutines` from `1.7.0-RC` to `1.7.1`
- updated `kotlinx-serialization` from `1.5.0` to `1.5.1`
- updated `wire` from `4.6.0` to `4.7.2`
- updated `ktor` from `2.3.0` to `2.3.1`

## r27

---

- **[breaking change]** updated `wire` from 4.5.3 to 4.6.0 (library consumers **must** also update `wire` to avoid issues)
- **[breaking change]** `extension-pics`: fixed JSON exception when Steam can give absolutely random order values for collections (`Int` -> `Double`)
- `core`: redone sign in process to extract Steam ID from JWT token (fixes QR login - btw I think that there is an easier way to know SteamID in QR logins...) 
- synchronized protobufs
- updated `ktor`, `kotlinx-serialization` and `coroutines`

## r26

---

- **[breaking change]** Kotlin Multiplatform support for `jvm`, `ios` (arm64/simulatorArm64/x64), `tvos` (arm64/simulatorArm64/x64) and `macos` (arm64/x64)
- **[breaking change]** `core`: `rootFolder` is replaced from `File` of JVM to `Path` of okio
- **[breaking change]** `core`, `extension-core`, `extension-guard`, `extension-pics`: removed AndroidX @Immutable/@Stable annotations from models, **this will lead to recomposition issues if used directly in Jetpack Compose** (wrap them in UI state objects)
- **[breaking change]** `extension-guard`: migrated from custom protobuf to .mafile-style JSON files (migration tools are provided in a separate module named `extension-guard-proto-migration`)
- **[breaking change]** `core`: `ktorProxyConfig` removed in favor of specifying your own Ktor `HttpClient` using `ktor { }` function in `kSteam { }` initializator
- `core`: refactored a lot of internal I/O-related code
- `core`: added `SteamClientConfiguration.AuthPrivateIpLogic.Generate` option for signing in (enabled by default on Apple platforms)
- `extension-guard`: rewritten java.nio/javax.crypto (bytebuffer/hmac) usages with okio
- `extension-pics`: collections are now filtered based on actual owned appids
- `extension-pics`: optimize metadata updating
- replaced `implementation` with `api` inside kSteam modules to prevent `ClassNotFoundException`