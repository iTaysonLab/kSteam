## r29

---

- streamline build process
- provide `android` artifacts as well, with Compose stability bindings

## r28

---

- **[breaking change]** redone `CMClientState` featuring new `AwaitingAuthorization` state (connected as a guest) and renamed `Logging` to `Authorizing` + `Idle` to `Offline`
- `core`: improved `CMClientState` exposing
- `core`: introduce `extractAccountIdFromSteam` and `toSteamId` extensions for `Long` and `Int`
- `core`: fixed rare Web API call crash
- `core/player`: make `getAchievementsProgress` and `getTopAchievements` public (proto API only for now)
- 
- `extension-pics`: optimized database lists, fixing `CallbackSuspendableMap must be initialized first!` crash
- `extension-core/news`: finalized parameters for `getUserNews`, introduced `UserNewsFilterScenario`
- `extension-core/news`
- updated `kotlin` from `1.8.20` to `1.8.21`
- updated `kotlinx-coroutines` from `1.7.0-RC` to `1.7.1`
- updated `kotlinx-serialization` from `1.5.0` to `1.5.1`
- updated `wire` from `4.6.0` to `4.7.0`
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