kSteam
---

kSteam is a **JVM/Android Kotlin library** which allows you to connect to the Steam network.

It's usage mostly based on **Kotlin Coroutines** and **states** to better suit for modern application development.

> This library is in very early state, so expect bugs and incomplete features.
> 
> Please note that this library is not endorsed, sponsored, allowed, developed by Valve Corporation or related to it. Don't report bugs to them!
---

### Goals
- Provide a easy-to-use library for accessing the Steam network on JVM/Android
- Make UI development easier by providing state-based approach without taking care of Protocol Buffers
- Manage high performance and low memory/storage footprint by using well-tested modern technology such as Wire (for protobufs) and Ktor (for networking)
- Provide full Steam Guard and new auth flow support
- Removing the gap between WebAPI and Steam3 messages
- Actively cache data for minimizing network usage and portability

### Credits
- [SteamKit](https://github.com/SteamRE/SteamKit/) and [JavaSteam](https://github.com/Longi94/JavaSteam/) - base for understanding how Steam3 network works
- [SteamDB protobuf repository](https://github.com/SteamDatabase/Protobufs/) is used in "models" module
- [Ktor](https://github.com/ktorio/ktor) is used as a networking library
- [Wire](https://github.com/square/wire) is used for protobuf decoding/encoding
- [Okio](https://github.com/square/okio) is used for cross-platform filesystem access
- [Exposed](https://github.com/jetbrains/exposed) and [H2 Database](https://github.com/h2database/h2database) is used for database access