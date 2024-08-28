Examples
---

### Create a kSteam client
```kotlin
// define a kSteam instance, this should be done once - multiple instances are not supported
val steamClient = kSteam {
    // location where kSteam will store its data like user accounts
    // this can be omitted on some platforms to use default directory
    rootFolder = [File]
    
    // device info shown when other users will manage sessions or approve this one
    deviceInfo = DeviceInformation(
        osType = EOSType.k_eAndroidUnknown, // current OS
        gamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_Phone, // device type
        platformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient, // don't change this
        deviceName = "kSteam device" // session name
    )
    
    // if you are using core-persistence module, initialize it here
    peristenceDriver = AndroidPersistenceDriver(applicationContext)
}

// if required, you can use a built-in dumper to analyze Steam Network packets
// they will be saved in %rootFolder%/dumps/%timestamp%/
steamClient.dumper.mode = PacketDumper.DumpMode.Full

// connect to the Steam network
// with core-persistence installed, kSteam can automatically sign in to first added account
steamClient.start() // - this is a suspending function that awaits up to first successful connection (not always fully authorized)
```
---
### Sign to the Steam Network (username + password)

```kotlin
// if not, you need to dispatch the sign-in process
val signResult = steamClient.account.signIn(
    username = "username",
    password = "password",
    rememberSession = false // - "true" will save the account so on the next launch, you could use hasSavedDataForAtLeastOneAccount()
)

if (signResult is AuthorizationResult.ProceedToTfa) {
    // 2FA, use steamClient.account.clientAuthState Flow to manage it
    steamClient.account.clientAuthState.collect { state ->
        // Update the UI
    }
} else if (signResult is AuthorizationResult.InvalidPassword) {
    // notify the user
}
```
---
### Send a raw message in the Steam Network
```kotlin
// will suspend and return a SteamPacket with a result
val resultPacket = steamClient.execute(
    // SteamPacket.newProto(...) can be used if the contents are protobuf
    SteamPacket(
        messageId = EMsg.k_EMsgClientHello,
        header = SteamPacketHeader.Protobuf(), // SteamPacketHeader.Binary is used for binary packets
        payload = packetPayload // ByteArray
    )
)
```

---

_The following examples will require to install the `extension-client` extension._

Installation is simple:

```kotlin
val extClient = kSteam {
    /* ... */
}.extendToClient(enablePics = true) // false if you don't need PICS
```

---
### Get the current user information
```kotlin
extClient.persona.currentPersona.onEach { persona ->
    println("I am ${persona.name}. My SteamID is ${persona.id}.")
}.launchIn(scope)
```

---
### Track currently playing game
```kotlin
extClient.persona.currentLivePersona()
    .map { it.ingame }
    .distinctUntilChanged()
    .collect { status ->
        when (status) {
            is Persona.IngameStatus.NonSteam -> {
                println("User is playing a non-Steam game: ${status.name}")
            }

            is Persona.IngameStatus.Steam -> {
                println("User is playing a Steam game with ID: ${status.appId}")
                // You can also access rich presence information if the game supports it
            }

            Persona.IngameStatus.None -> {
                println("User is not playing anything.")
            }
        }
    }
```