kSteam Common Protobufs
---

This module contains dumped `.proto` files that are tracked from [SteamDB protobuf repository](https://github.com/SteamDatabase/Protobufs/).

Original protobuf files are modified at the moment by adding `option java_package`.

### Updating proto files
1. Update the `Protobufs` submodule
2. Run `gradlew proto-common:upgradeProtoFiles`
3. Run `gradlew proto-common:generateProtos`