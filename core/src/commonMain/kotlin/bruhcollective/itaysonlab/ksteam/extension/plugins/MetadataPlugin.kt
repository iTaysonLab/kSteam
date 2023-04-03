package bruhcollective.itaysonlab.ksteam.extension.plugins

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary

/**
 * This "plugin" allows independent kSteam extensions to access "cached" metadata services.
 *
 * Any [Handler] might implement this interface, but the first added in order will be used.
 *
 * For example:
 * Official PICS implementation (extension-pics) uses this plugin to expose it's "cached" metadata to Store handler (extension-core).
 * While the Store handler already has a inbuilt memory cache, using this metadata (for owned apps):
 * - reduces extra API calls
 * - reduces extra memory usage (non-duplicating metadata)
 */
interface MetadataPlugin {
    /**
     * Get mapped [AppSummary] for given [AppId]
     *
     * @return mapped [AppSummary] by their [AppId]'s
     */
    suspend fun getMetadataFor(appIds: List<AppId>): Map<AppId, AppSummary>
}