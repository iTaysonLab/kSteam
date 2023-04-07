kSteam Guard Protobuf Migration extension
---
Allows kSteam to migrate r25- `extension-guard` Steam Guard files to the new universal JSON format.

### Installation

```kotlin
kSteam {
    install(GuardProtoMigration)
}
```

### Usage

```kotlin
kSteamClient.guard.tryMigratingProtobufs(kSteamClient) // suspend
```