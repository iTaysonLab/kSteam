kSteam PICS extension
---
Provides kSteam with an ability to request owned application metadata by using licenses/PICS system. Also provides a library collections manager API.

**Requires installation of Core extension**

### Installation

```kotlin
kSteam {
    install(Core)
    
    install(Pics) {
        // A key-value database, you can use MMKV on Android JVM.
        database = YourKvDatabaseImplementation()
    }
}
```

### [Android only] MMKV

If you plan to use kSteam on a Android device, you can skip the KV implementation with MMKV. This is a key-value database developed by Tencent.

TODO: describe implementation

### What is covered
- Pics
- Library

### TODO
- The library is still work in progress, expect bugs and non-implemented code