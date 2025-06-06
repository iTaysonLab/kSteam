## r43

- **Even faster library!** PICS chunk processing is now faster, decreasing initial startup time even more.
- 

## r42

- **Supercharged library queries!** For UI display reasons, you might not need to query ALL the data from applications. 
- - Now, kSteam adds a `full` argument to most of the methods in `Library` that can skip extra information.
- - **kSteam Library Queries** got new parameters: `alwaysFetchLicenses`, `alwaysFetchPlayTime` and `fetchFullInformation`. All of them are false by default.
- - `fetchFullInformation` or `full` set to false will skip querying the following metadata: content descriptors, tags, categories, localized assets, developers, publishers and franchises
- **Improved PICS Database!** With new fields included in SQLite tables, kSteam does no longer store any VDF at all, greatly improving update speeds and reducing database sizes.
- `SteamApplication`: Content Descriptors are now parsed properly
- `Library`: SQL query building is fixed for some collections and simple requests
- `Library`: Added `KsLibraryQuerySortBy.NormalizedName` for sorting based on `sortas` field in AppInfo with fallback to regular names

## r41

- **Rewritten network communication!** Inspired by job system in other Steam Network clients, the CMJob system in kSteam allows for more streamlined experience and better stability when working with the Steam Network.
- - kSteam also introduces **Streaming CM Jobs**, which allow for processing every received part of a multi-message job, instead of collecting to List first. This really helps with PICS, where this new method greatly reduces memory usage on updates.
- **Major PICS rewrite!** The whole database is now powered by Room (SQLite ORM by Google), which enables following things:
- - Kotlin 2.1 support and beyond (Realm is currently "frozen")
- - More platforms supported (kSteam still primarily supports only JVM + Android)
- - Rewritten startup sequence, which should be more accurate in multi-user environments
- - A lot of internal DB objects were rewritten from scratch to improve performance and decrease memory usage
- **Jetpack Compose bindings removed.** Refer to `Jetpack Compose` section on README on how to improve stability when using kSteam models in `@Composable`.
- **Synchronized Protobuf files and dependencies.**
- `UserNews`: statuses support
- `Account`: IP fetching is now on separate thread to "fix" NetworkOnMainThreadException on Android

## r40

- **kSteam Library Queries!** This all-new system interops with Steam Library Collections (while adding new filters), allowing to fetch user's game library with play time and license support.
- **Multi-license support!** Now kSteam understands package licenses and can apply them to app queries - you can easily get information about multiple copies and who own them.

## r39

- **[breaking change]**: `ExtendedSteamClient` now implements the new abstract `SteamClient` interface, while the old `SteamClient` is now internal and named `SteamClientImpl`.  
- **[breaking change]**: `extension-client/published-files`: all-new `PublishedFile` class that features more mapped variables and improved code style
- **[breaking change]**: `core`: gRPC client `authenticationClient` renamed to `authentication`
- `core`: move gRPC clients from `extension-client`'s `ExtendedSteamGrpcClients` to `SteamGrpcClients`
- `extension-client/published-files`: implemented `getFilesAppList` and `getFiles`
- `extension-client/published-files`: implemented `EPublishedFileInfoMatchingFileType` and `EWorkshopFileType` enums
- `proto-common`: merge protobuf files from `core` and `extension-client`
- `extension-client`: implemented `ClientCommunication` for remote operations
- `core`: add `ClientCommService`, `FamilyGroupsService`, `GameNotesService`, `StoreService`, `MobileDeviceService`, `UserGameNotesService` to `SteamGrpcClients`
- `proto-common`: update to latest protobufs
- `proto-common`: don't embed .proto files in JAR

## r38

---

- **[breaking change]** `extension-client/guard`: `getActiveSessions` now returns an `ActiveSessions` object that separates current kSteam session and others
- **[breaking change]** `extension-client`: implemented separate databases, "ks_shared.realm" for PICS and "ks_<ID>.realm" for user-specific items (personas, for example)
- **[breaking change]** `extension-client`: `Persona` is now rebuilt, featuring built-in relationship indicator and new `Status` interface
- **[breaking change]** `core`: `trySignInSaved` is now separated into `trySignInSaved(SteamId)` and `trySignInSavedDefault()`
- `core`, `extension-client/guard`: replaced `while (true)` in flows with `while (currentCoroutineContext().isActive)`
- `extension-client/guard`: fixed generation of revocation signature
- `core`: add `Account.getWebCookies` for fetching required cookies to access secured Steam pages in a WebView
- `extension-client/guard`: make `MobileConfirmationItem.icon` nullable to fix parsing when a confirmation does not have it (phone number changes)д
- `core-persistence`: add `secureGetSteamIds` and improve `secureContainsIdentity`
- `core`: implemented support for expired sessions
- `core`: implemented auto-restoration of autologin
- `extension-client`: implemented new rich presence formatter that automatically updates the database
- `extension-client`: provide `SteamRegexes` to help QR scanning implementations 

## r37

---

- **[breaking change]** `core`: handler system was removed out in favour for static injection of subsystems
- **[breaking change]** `extension-client`: due to handler system being removed, extension is now installed as a Kotlin extension for `SteamClient` (see `extendToClient` function)
- **[breaking change]** `extension-client/guard-management`: `AwaitingSession` is now `IncomingSession`
- **[breaking change]** `extension-client/guard-management`: `IncomingSession` and `ActiveSession` are now `Serializable`
- **[breaking change]** `extension-client/guard-management`: reworked API to be more consistent
- **[breaking change]** `extension-client/guard`: major refactor of creation/moving process, removing the state system
- `core`: implement shared gRPC client storage
- `core`: streamlined coroutines scope creation/usage
- `core`: remove embedded libdeflate for native platforms multi-message support because Okio has support for gzip streams for K/N
- `core`: improved performance by using an enum cache for `EMsg` and `EResult`
- `extension-client`: implement shared gRPC client storage for easy invoking of supported autogenerated RPC interfaces
- `extension-client/guard`: fixed generating signature for revoking sessions
- `extension-client/guard-management`: added a method to revoke current session
- update dependencies

## r34

---

- `core/account` now tries to search Steam Guard code for all bound extensions
- **[breaking change]** `extension-client` handler extensions for PICS/Library/Guard are now moved back to `bruhcollective.itaysonlab.ksteam.handlers` package

## r32

---

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

---

- **[breaking change]** Disable support for macOS/iOS compilation, effectively making kSteam a JVM/Android-only library
- **[breaking change]** RPC calls now use Wire's gRPC custom client implementation instead of manually passing request/response adapters. This also enables usage of Steam-defined services directly.
- `extension-pics`: support for 64-bit app IDs, making the client not crashing if providing folders with non-Steam applications 
- update dependencies
- streamline the version updating procedure by introducing version catalogs
- synchronized protobufs with the latest provided by Steam

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