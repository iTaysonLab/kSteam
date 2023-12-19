kSteam Client Extension
---
Contains all user-friendly Kotlin/Steam API implementations. Inspired by ""

_This extension heavily relies on Realm Mobile Database (known also as MongoDB Atlas) for active data caching, starting from user profiles and ending with game metadata. In future versions of the module, this may be lifted for users to provide their own database solutions. Realm is supported on Android, native macOS/iOS and JVM (including Windows)._ 

### Installation

```kotlin
kSteam {
    install(KsteamClient) {
        // if you don't need user library, disable PICS for faster startup
        enablePics = true
    }
}
```

### API coverage

**General usage**

| Feature        | Status | Description                                                                               |
|----------------|--------|-------------------------------------------------------------------------------------------|
| Persona        | Tier 2 | Information about Steam users: avatars, usernames, activity information                   |
| CurrentPersona | Tier 1 | Live information about currently signed-in Steam profile                                  |
| Notifications  | Tier 2 | Steam Notifications, used in the official mobile app or new desktop client                |
| Profile        | Tier 2 | Information (and editing) about Steam user profile pages: widgets, customizations, themes |
| News           | Tier 2 | Steam News content                                                                        |
| Player         | Tier 3 | General library and "Play Next" queue not tied to PICS infrastructure                     |
| PublishedFiles | Tier 3 | Workshop content, game guides, screenshots, videos                                        |
| Store          | Tier 3 | Steam Store apps metadata access                                                          |
| UserNews       | Tier 2 | "Friends Activity" content                                                                |

**Steam Guard**

| Feature           | Status | Description                                                    |
|-------------------|--------|----------------------------------------------------------------|
| Guard             | Tier 1 | Setup, manage and use Steam Guard (with sign-in notifications) |
| GuardConfirmation | Tier 1 | Trade/Market confirmations via legacy API                      |
| GuardManagement   | Tier 2 | Session managing (signed-in devices)                           |

**Library**
_requires PICS to be enabled in configuration_

| Feature         | Status | Description                                         |
|-----------------|--------|-----------------------------------------------------|
| Library         | Tier 2 | User library: collections, "Home" page, owned games |
| Pics            | Tier 2 | PICS: extended metadata of purchased apps/bundles   |

### About tier system

- **Tier 1** means first-class implementation. This means higher stability among other features, proper documented fields/methods and decent coverage of actual Steam service behind the handler.
- **Tier 2** means "finalization in progress". This means average stability and documentation, but the Steam coverage should be at least stable (or almost fully covered).
- **Tier 3** means "heavy WIP". This means low stability, Steam API coverage or lack of proper documentation.
- **Tier 4** means "planned". There is no actual available handler to use, yet - however, being listed in this table means a significantly lower risk of cancelling the implementation.

The tier system is linked to the Cobalt application, which acts as a "live testing playground" for developing kSteam. So, if Cobalt has a proper friend system - it means that `FriendList` feature is at least `Tier 2`.