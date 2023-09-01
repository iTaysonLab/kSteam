kSteam Guard extension
---
Allows kSteam to generate TOTP codes for Steam Guard, managing trade/sell confirmations and online sessions.

### Installation

```kotlin
kSteam {
    install(Guard) {
        // an example, please use an appropriate UUID generator or leave default
        uuid = "UUID1234567890"
    }
}
```

### What is covered
- Guard
- GuardConfirmation
- GuardManagement

### TODO
- Cancelling session does not work because of signature generation - need to reverse engineer the official Steam Mobile app once again
- Other features should work great (we still need to implement some sort of automated tests through)