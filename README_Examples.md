Examples
---

### Create a kSteam client
```kotlin
// define a kSteam instance, this should be done once - multiple instances are not supported
val steamClient = kSteam {
    // location where kSteam will store its data like user accounts
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
    
    // if you are using client module, initialize it here as well
    install(KsteamClient) {
        // specify your device UUID
        uuid = ""
        
        // if you need to modify RealmDB configuration
        realmDatabaseConfiguration = ""
    }
    
    // include any other extensions
}

// if required, you can use a built-in dumper to analyze Steam Network packets
// they will be saved in %rootFolder%/dumps/%timestamp%/
steamClient.dumperMode = PacketDumper.DumpMode.Full

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

if (signResult is AuthorizationResult.Success) {
    // 2FA, use steamClient.account.clientAuthState Flow to manage it
    steamClient.account.clientAuthState.collect { state ->
        // Update the UI
    }
} else if (signResult is AuthorizationResult.InvalidPassword) {
    // notify the user
}
```
---

_The following examples will require to install the `client` extension._

---
### Get the current user information
```kotlin
steamClient.persona.currentPersona.onEach { persona ->
    println("I am ${persona.name}. My SteamID is ${persona.id}.")
}.launchIn(scope)
```
---
### Get the store item information
```kotlin

```