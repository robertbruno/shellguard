package dev.shellguard

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Lists and restores files sitting in the MediaStore trash.
 *
 * This is recovery of *trashed* items, not forensic carving. Android keeps
 * media deleted through the MediaStore API for a grace period (30 days by
 * default) before purging it; until then the rows are still addressable and
 * can be untrashed with user consent.
 *
 * What this cannot do, and why:
 *  - Files deleted before this grace period expired are gone from the index.
 *  - Files on internal storage that bypassed MediaStore leave no row to find.
 *  - Raw block-level carving (testdisk-style) is useless on internal storage:
 *    file-based encryption derives a per-file key that the system discards on
 *    delete, so recovered blocks decrypt to nothing. Carving only pays off on
 *    unencrypted removable media, which needs root and direct block access.
 */
object RecoveryStore {

    /** Trash retention is 30 days on current Android versions. */
    private val RETENTION = TimeUnit.DAYS.toMillis(30)

    data class TrashedItem(
        val id: Long,
        val uri: android.net.Uri,
        val displayName: String,
        val sizeBytes: Long,
        val mimeType: String?,
        /** Epoch millis when the item was trashed, or null if unreported. */
        val trashedAt: Long?,
        /** Epoch millis when the system may purge it, or null if unknown. */
        val expiresAt: Long?,
    )

    /** True when this device exposes the trash API at all. */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Returns the trashed media this app is allowed to see, newest first.
     * Empty when unsupported or when the read permission has not been granted.
     */
    suspend fun listTrashed(context: Context): List<TrashedItem> =
        withContext(Dispatchers.IO) {
            if (!isSupported) return@withContext emptyList()

            val resolver = context.contentResolver
            val results = mutableListOf<TrashedItem>()

            // Images, video and audio live in separate collections.
            val collections = listOf(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            )

            for (collection in collections) {
                results += queryTrashed(resolver, collection)
            }

            results.sortedByDescending { it.trashedAt ?: 0L }
        }

    private fun queryTrashed(
        resolver: ContentResolver,
        collection: android.net.Uri,
    ): List<TrashedItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_EXPIRES,
        )

        // MATCH_ONLY returns exclusively trashed rows; the default hides them.
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        return try {
            resolver.query(collection, projection, args, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val expiresCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)

                val items = mutableListOf<TrashedItem>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)

                    // DATE_EXPIRES is in seconds, not millis, and may be null.
                    val expiresAt = if (cursor.isNull(expiresCol)) {
                        null
                    } else {
                        TimeUnit.SECONDS.toMillis(cursor.getLong(expiresCol))
                    }

                    items += TrashedItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = cursor.getString(nameCol) ?: "(sin nombre)",
                        sizeBytes = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol),
                        trashedAt = expiresAt?.minus(RETENTION),
                        expiresAt = expiresAt,
                    )
                }
                items
            } ?: emptyList()
        } catch (e: SecurityException) {
            // Read permission missing or revoked mid-session.
            Log.w("ShellGuard", "No permission to read trashed media", e)
            emptyList()
        } catch (e: Exception) {
            Log.w("ShellGuard", "Trash query failed for $collection", e)
            emptyList()
        }
    }

    /**
     * Builds the system consent dialog for restoring [uris]. The caller must
     * launch the returned IntentSender and handle the result: restoring media
     * owned by another app always requires explicit user approval, so this
     * cannot complete silently.
     *
     * Returns null when unsupported or when the request could not be built.
     */
    fun buildRestoreRequest(
        context: Context,
        uris: Collection<android.net.Uri>,
    ): android.content.IntentSender? {
        if (!isSupported || uris.isEmpty()) return null
        return try {
            MediaStore.createTrashRequest(
                context.contentResolver,
                uris.toList(),
                false, // false = untrash
            ).intentSender
        } catch (e: Exception) {
            Log.w("ShellGuard", "Could not build restore request", e)
            null
        }
    }
}
