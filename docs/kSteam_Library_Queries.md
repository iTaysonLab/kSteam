## kSteam Library Queries
_Requires kSteam r40+ and attached `extension-client` with PICS enabled._

Welcome to kSteam Library Queries! This all-new system provides developers a new ability to quickly and efficiently fetch information about user's game library.

```kotlin
// Compose the query
val query = KsLibraryQueryBuilder()
    // Type
    .withAppType(EAppType.Game)
    // Genres
    .withGenre(EGenre.Action)
    .withGenre(EGenre.Adventure)
    // Store Categories
    .withStoreCategory(EStoreCategory.TradingCard)
    .withStoreCategory(EStoreCategory.GamepadPreferred)
    .withStoreCategories(EStoreCategory.PS5ControllerSupport, EStoreCategory.PS5ControllerBTSupport)
    // Owner Filter
    .withOwnerFilter(KsLibraryQueryOwnerFilter.OwnedOnly)
    // Sorting
    .sortBy(KsLibraryQuerySortBy.MetacriticScore, KsLibraryQuerySortByDirection.Descending)
    .build()

// This query will return:
// - games
// - of genres "Action" and "Adventure"
// - that have trading cards
// - that have a "Gamepad preferred" mark
// - support wired and wireless DualSense controllers
// - that current account actually owns, excluding games that are only shared
// - sorted by Metacritic score in descending order
kSteamExtended.library.execute(query)
```

---

## Filters

### Filtering by application type

```kotlin
// Return only games
query.withAppType(EAppType.Game)

// Return games and DLCs
query.withAppType(EAppType.Game).withAppType(EAppType.DLC)
```

---

### Filtering by genres

```kotlin
// Return only action games
query.withGenre(EGenre.Action)

// Return only action-adventure games
query.withGenre(EGenre.Action).withGenre(EGenre.Adventure)
```

---

### Filtering by store categories

```kotlin
// Return only items that supports trading cards
query.withStoreCategory(EStoreCategory.TradingCard)

// Return only items that supports trading cards and wirelessly connected PS5 controllers
query.withStoreCategory(EGenre.Action).withStoreCategory(EGenre.PS5ControllerBTSupport)

// Return only items that supports trading cards OR wirelessly connected PS5 controllers
query.withStoreCategories(EGenre.Action, EGenre.PS5ControllerBTSupport)
```

---

### Filtering by purchase status

```kotlin
// By default, KsLibraryQueryOwnerFilter.Default is used, that allows shared copies

// Return only items that were purchased by this user
query.withOwnerFilter(KsLibraryQueryOwnerFilter.OwnedOnly)
```

---

### Filtering by purchase status

```kotlin
// By default, KsLibraryQueryOwnerFilter.Default is used, that allows shared copies

// Return only items that were purchased by this user
query.withOwnerFilter(KsLibraryQueryOwnerFilter.OwnedOnly)
```

---

### Filtering by play state

```kotlin
// Return items that were never launched by the user. Applies only for games.
query.withPlayState(EPlayState.PlayedNever)
```

---

### Filter by controller support

```kotlin
// Return items that fully support (Xbox) controllers
// Note that DualShock, DualSense and Steam Input API support should be requested by withStoreCategory
query.withControllerSupport(KsLibraryQueryControllerSupportFilter.Full)
```

---

### Filter by master package support

```kotlin
// Return items that are available on EA Play subscription
query.withMasterSubscriptionPackage(1289670)
```

---

### Filter by Steam Deck support

```kotlin
// Return items that are marked as "Playable" or "Verified" on Steam Deck
query.withSteamDeckMinimumSupport(ESteamDeckSupport.Playable)
```

---

### Filter by store tags

```kotlin
// Return items that are marked as "Story Rich" and "Multiple Endings"
query.withStoreTags(1742, 6971)
```

---

## Other options

### Sorting

```kotlin
// Sort by application name in ascending order
// NOTE: Due to issues on Realm side, this can ignore non-Latin characters.
// This only sorted by non-localized (English) names at the moment
query.sortBy(KsLibraryQuerySortBy.Name, KsLibraryQuerySortByDirection.Ascending)


// Sort by Metacritic score in descending order (100-0) 
query.sortBy(KsLibraryQuerySortBy.MetacriticScore, KsLibraryQuerySortByDirection.Descending)
```

---

### Search by name

```kotlin
// Return items that contain "Half-Life" in their name
// This is case-insensitive, however due to issues on Realm side non-Latin characters are counted as different
// This only searches non-localized (English) names at the moment
query.withSearchQuery("Half-Life")
```

---

### Limit results

```kotlin
// Only return first 5 items
query.limit(5)
```

---