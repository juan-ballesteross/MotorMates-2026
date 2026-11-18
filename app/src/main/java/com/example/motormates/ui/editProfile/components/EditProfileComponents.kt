package com.example.motormates.ui.editProfile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.GarageVehicleUi
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

private const val BIO_MAX_LENGTH = 150
private val COVER_HEIGHT = 150.dp
private val AVATAR_SIZE = 92.dp

@Composable
fun EditProfileTopBar(
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
    ) {
        IconButton(onClick = onCloseClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.edit_profile_close_cd),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = stringResource(R.string.edit_profile_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(end = Spacing.md)
                        .size(20.dp)
                )
            } else {
                TextButton(onClick = onSaveClick, modifier = Modifier.testTag("editProfileSave")) {
                    Text(
                        text = stringResource(R.string.edit_profile_save_button),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Portada con la foto de perfil encima, como se ve en el perfil. Tocar la
 * portada cambia la portada; tocar la foto (o su camarita) cambia la foto.
 * Al lado de la foto se ve en vivo cómo queda el nombre y el usuario.
 */
@Composable
fun EditProfileHeader(
    coverImageUrl: String?,
    profileImageUrl: String?,
    fullName: String,
    username: String,
    isCoverUploading: Boolean,
    isPhotoUploading: Boolean,
    onChangeCoverClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(COVER_HEIGHT + AVATAR_SIZE / 2)
    ) {
        // Portada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(COVER_HEIGHT)
                .clip(RoundedCornerShape(Radius.xl))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !isCoverUploading, onClick = onChangeCoverClick)
                .testTag("editProfileCover")
        ) {
            if (coverImageUrl == null) {
                Image(
                    painter = painterResource(R.drawable.profile_cover),
                    contentDescription = stringResource(R.string.user_cover_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = coverImageUrl,
                    contentDescription = stringResource(R.string.user_cover_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Degradado abajo para que la foto de perfil y el botón resalten
            // sobre cualquier portada.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f)
                        )
                    )
            )
            OverlayPill(
                icon = Icons.Filled.PhotoCamera,
                text = stringResource(
                    if (isCoverUploading) R.string.edit_profile_uploading_cover
                    else R.string.edit_profile_change_cover_button
                ),
                onClick = onChangeCoverClick,
                enabled = !isCoverUploading,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
            )
            if (isCoverUploading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
        }

        // Foto de perfil, montada sobre el borde de la portada.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(AVATAR_SIZE)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = !isPhotoUploading, onClick = onChangePhotoClick)
                        .testTag("editProfileAvatar"),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = stringResource(R.string.user_avatar_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    if (isPhotoUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                // Camarita: deja claro que la foto se puede cambiar.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = !isPhotoUploading, onClick = onChangePhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.edit_profile_change_photo_button),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.padding(bottom = Spacing.xs).weight(1f)) {
                Text(
                    text = fullName.ifBlank { "Tu nombre" },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@" + username.ifBlank { "usuario" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Botón translúcido sobre una foto (siempre oscuro, no depende del tema). */
@Composable
private fun OverlayPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Tarjeta de sección: título con ícono y el contenido debajo. */
@Composable
fun EditProfileSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        content()
    }
}

/**
 * Campo del formulario de perfil. Vive dentro de una tarjeta (surface), así
 * que el campo usa el fondo de la pantalla para que se distinga.
 */
@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supportingText: String? = null,
    testTag: String? = null
) {
    Column(modifier = modifier.padding(bottom = Spacing.sm)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
            shape = RoundedCornerShape(Radius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}

/** Nombre, usuario, ubicación y biografía, dentro de su tarjeta. */
@Composable
fun EditProfileInfoCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    EditProfileSectionCard(title = "Información personal", icon = Icons.Filled.Person, modifier = modifier) {
        EditProfileField(
            label = stringResource(R.string.edit_profile_fullname_label),
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = stringResource(R.string.edit_profile_fullname_placeholder),
            testTag = "editProfileFullName"
        )
        EditProfileField(
            label = stringResource(R.string.edit_profile_username_label),
            value = username,
            onValueChange = onUsernameChange,
            placeholder = stringResource(R.string.edit_profile_username_placeholder),
            leadingIcon = Icons.Filled.AlternateEmail,
            testTag = "editProfileUsername"
        )
        EditProfileField(
            label = stringResource(R.string.edit_profile_location_label),
            value = location,
            onValueChange = onLocationChange,
            placeholder = stringResource(R.string.edit_profile_location_placeholder),
            leadingIcon = Icons.Filled.LocationOn
        )
        EditProfileField(
            label = stringResource(R.string.edit_profile_bio_label),
            value = bio,
            onValueChange = { if (it.length <= BIO_MAX_LENGTH) onBioChange(it) },
            placeholder = stringResource(R.string.edit_profile_bio_placeholder),
            singleLine = false,
            minLines = 3,
            supportingText = stringResource(R.string.edit_profile_bio_counter, bio.length, BIO_MAX_LENGTH),
            modifier = Modifier.padding(bottom = 0.dp)
        )
    }
}

/**
 * Mi garaje: los vehículos publicados, cada uno con su lápiz para editar,
 * y el atajo para publicar otro.
 */
@Composable
fun EditProfileGarageCard(
    cars: List<GarageVehicleUi>,
    isLoading: Boolean,
    onAddVehicleClick: () -> Unit,
    onEditVehicleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    EditProfileSectionCard(
        title = stringResource(R.string.edit_profile_garage_title),
        icon = Icons.Filled.DirectionsCar,
        modifier = modifier,
        trailing = {
            if (cars.isNotEmpty()) {
                CountBadge(count = cars.size)
                Spacer(modifier = Modifier.width(Spacing.xs))
                TextButton(onClick = onAddVehicleClick, modifier = Modifier.testTag("editProfileAddVehicle")) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }

            cars.isEmpty() -> GarageEmpty(onAddVehicleClick = onAddVehicleClick)

            else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                cars.forEach { car ->
                    GarageEditRow(car = car, onEditClick = { onEditVehicleClick(car.id) })
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text = count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GarageEmpty(onAddVehicleClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.md))
            .padding(vertical = Spacing.lg, horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Aún no has publicado ningún carro",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Publícalo para que salga en tu garaje y otros puedan compararlo con el suyo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(
            onClick = onAddVehicleClick,
            shape = RoundedCornerShape(Radius.md),
            modifier = Modifier.testTag("editProfileAddVehicle")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publicar mi vehículo")
        }
    }
}

/** Un carro del garaje: foto, nombre, año·categoría, specs y el lápiz para editarlo. */
@Composable
private fun GarageEditRow(car: GarageVehicleUi, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onEditClick)
            .padding(Spacing.xs)
            .testTag("editProfileGarageItem_${car.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbModifier = Modifier
            .size(width = 96.dp, height = 68.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        if (car.imageUrl != null) {
            AsyncImage(model = car.imageUrl, contentDescription = car.title, contentScale = ContentScale.Crop, modifier = thumbModifier)
        } else {
            Box(modifier = thumbModifier, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = car.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = car.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
            if (car.potencia != null || car.aceleracion != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    car.potencia?.let { SpecChip(icon = Icons.Filled.Speed, text = it) }
                    car.aceleracion?.let { SpecChip(icon = Icons.Filled.Timer, text = "0-100 $it") }
                }
            }
        }
        IconButton(onClick = onEditClick) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.edit_profile_edit_vehicle_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, maxLines = 1)
    }
}

/** Cerrar sesión, discreto al final: no compite con Guardar. */
@Composable
fun EditProfileLogoutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("editProfileLogout")
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = stringResource(R.string.edit_profile_logout_button),
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Mensaje de error de una sección (subida de foto, guardado). */
@Composable
fun EditProfileError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
