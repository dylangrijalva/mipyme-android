package co.mipyme.screen.product_editor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import co.mipyme.R
import co.mipyme.ui.Moshi
import co.mipyme.ui.component.MoshiMonogram
import co.mipyme.ui.component.MoshiIconButton
import co.mipyme.ui.component.MoshiBottomSheet
import co.mipyme.ui.component.MoshiListItem
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val PHOTO_CACHE_DIRECTORY = "product-photos"

class ProductPhotoFileProvider : FileProvider()

internal data class ProductEditorPhotoActions(
    val takePhoto: () -> Unit,
    val choosePhoto: () -> Unit,
    val discardCapture: () -> Unit,
    val isPicking: Boolean,
)

@Composable
internal fun rememberProductEditorPhotoActions(
    onPhoto: (Uri) -> Boolean,
    onError: () -> Unit,
): ProductEditorPhotoActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val directory = remember(context) { File(context.cacheDir, PHOTO_CACHE_DIRECTORY) }
    val authority = "${context.packageName}.product-photo"
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var ownedGalleryGrant by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingGallery by rememberSaveable { mutableStateOf(false) }
    var preparingCapture by remember { mutableStateOf(false) }

    fun discardSelection(keepGallery: Uri? = null) {
        selectedCapture?.let { File(directory, it).delete() }
        selectedCapture = null
        val ownedGrant = ownedGalleryGrant?.let(Uri::parse)
        if (ownedGrant != null && ownedGrant != keepGallery) {
            try {
                context.contentResolver.releasePersistableUriPermission(ownedGrant, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // The system or provider may have already revoked this grant.
            }
            ownedGalleryGrant = null
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val name = pendingCapture
        pendingCapture = null
        if (name != null) {
            val file = File(directory, name)
            val hasImage = captured && file.length() > 0
            if (hasImage && onPhoto(FileProvider.getUriForFile(context, authority, file))) {
                discardSelection()
                selectedCapture = name
            } else {
                file.delete()
                if (captured && !hasImage) onError()
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickingGallery = false
        if (uri != null && onPhoto(uri)) {
            discardSelection(keepGallery = uri)
            val resolver = context.contentResolver
            if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) {
                try {
                    resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ownedGalleryGrant = uri.toString()
                } catch (_: SecurityException) {
                    // Some gallery providers give read access for only the current session.
                }
            }
        }
    }

    return ProductEditorPhotoActions(
        takePhoto = {
            if (!preparingCapture && pendingCapture == null) {
                preparingCapture = true
                scope.launch {
                    val file = File(directory, "product-${UUID.randomUUID()}.jpg")
                    val uri = withContext(Dispatchers.IO) {
                        try {
                            if (directory.isDirectory || directory.mkdirs()) {
                                FileProvider.getUriForFile(context, authority, file)
                            } else {
                                null
                            }
                        } catch (_: IllegalArgumentException) {
                            null
                        } catch (_: SecurityException) {
                            null
                        }
                    }
                    preparingCapture = false
                    if (uri == null) {
                        onError()
                    } else {
                        pendingCapture = file.name
                        try {
                            camera.launch(uri)
                        } catch (_: ActivityNotFoundException) {
                            pendingCapture = null
                            onError()
                        } catch (_: SecurityException) {
                            pendingCapture = null
                            onError()
                        }
                    }
                }
            }
        },
        choosePhoto = {
            pickingGallery = true
            try {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: ActivityNotFoundException) {
                pickingGallery = false
                onError()
            } catch (_: SecurityException) {
                pickingGallery = false
                onError()
            }
        },
        discardCapture = { discardSelection() },
        isPicking = preparingCapture || pendingCapture != null || pickingGallery,
    )
}

@Composable
internal fun ProductEditorPhotoTile(
    photoUri: Uri?,
    photoUrl: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    name: String,
) {
    val spacing = Moshi.spacing
    val photo = photoUri ?: photoUrl
    val label = stringResource(
        if (photo == null) R.string.product_editor_add_photo else R.string.product_editor_change_photo,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(spacing.space12 + spacing.space2))
        val imageModifier = Modifier.size(spacing.space16 * 2)
        if (photo == null) {
            MoshiMonogram(name = name, modifier = imageModifier)
        } else {
            AsyncImage(
                model = photo,
                contentDescription = null,
                modifier = imageModifier.clip(RoundedCornerShape(spacing.space2)),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(spacing.space2))
        MoshiIconButton(
            icon = R.drawable.ic_fluent_image_add_24_regular,
            label = label,
            onClick = onClick,
            enabled = enabled,
            tint = Moshi.colors.foreground.secondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductEditorPhotoSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    MoshiBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(
            if (hasPhoto) R.string.product_editor_change_photo else R.string.product_editor_add_photo,
        ),
    ) {
        MoshiListItem(label = stringResource(R.string.product_editor_take_photo), onClick = onTakePhoto)
        MoshiListItem(label = stringResource(R.string.product_editor_choose_photo), onClick = onChoosePhoto)
        if (hasPhoto) {
            MoshiListItem(
                label = stringResource(R.string.product_editor_remove_photo),
                onClick = onRemovePhoto,
                destructive = true,
            )
        }
    }
}
