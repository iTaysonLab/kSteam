**Persistence**
1. iOS: enable support for auto-polling data from Keychain in case of full reinstall

How it works: Keychain persists data even if application is uninstalled. kSteam saves all required information inside the Keychain object, however in case of full reinstall, autologin information will be wiped. 

How it can be done: Search and return first Keychain item with matching service name, if > 0 - get the first one and set autologin SteamID.