kSteam Core
---
This module is actually what makes kSteam possible - a CM client, Web API and some other utilities.

### Usage

```kotlin
kSteam {
    // Core in this context is extension-core, it is possible to use kSteam without it for a very bare-bones experience.
    install(Core)
}
```

### Included handlers
- Account
- WebApi