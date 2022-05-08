package `in`.windrunner.android_toolbox.tools

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile

object UriContentUtil {

    fun getFileName(context: Context, uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_FILE -> uri.toFile().name
        ContentResolver.SCHEME_CONTENT -> getContentUriName(context, uri)
        else -> null
    }

    private fun getContentUriName(context: Context, uri: Uri): String? =
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                val displayNameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return cursor.getString(displayNameColumn)
            } else null
        }
}