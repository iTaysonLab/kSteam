kSteam extensions
---
To help making kSteam multi-platform and improve the codebase, kSteam introduces an **extension subsystem**.

### Usage

```kotlin
kSteam {
    // Installs the Core extension
    install(Core)
    
    // Installs the Pics extension
    install(Pics) {
        keyValueDatabase = MmkvKeyValueDatebase()
    }
}
```