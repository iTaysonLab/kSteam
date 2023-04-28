## r27

---

- **[breaking change]** updated `wire` from 4.5.3 to 4.5.6 (library users **must** also update `wire` to avoid issues)
- **[breaking change]** `extension-pics`: fixed JSON exception when Steam can give absolutely random order values for collections (`Int` -> `Double`)
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