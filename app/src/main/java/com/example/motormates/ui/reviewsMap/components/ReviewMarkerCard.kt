package com.example.motormates.ui.reviewsMap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.motormates.R
import com.example.motormates.data.model.ReviewMapMarkerUi
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/**
 * La información de la reseña que aparece al tocar su marcador.
 *
 * Es una tarjeta propia y no el "info window" que trae Google: ese se pinta
 * con el estilo del sistema (burbuja blanca) y chocaría con la paleta oscura
 * de la app, además de no poder usar Coil para la foto del autor.
 */
@Composable
fun ReviewMarkerCard(
    marker: ReviewMapMarkerUi,
    modifier: Modifier = Modifier,
    onCardClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reviewMarkerCard")
            .clip(RoundedCornerShape(Radius.lg))
            .background(MaterialTheme.colorScheme.surface)
            // Al tocar la tarjeta se va al detalle del vehículo reseñado.
            .clickable { onCardClick(marker.vehicleId) }
            .padding(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // La foto del carro, la misma del marcador: así se reconoce cuál se tocó.
            VehiclePhoto(imageUrl = marker.vehicleImageUrl, name = marker.vehicleName)

            Spacer(modifier = Modifier.width(Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.vehicleName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatar(avatarUrl = marker.avatarUrl, name = marker.nombreUsuario)
                    Spacer(modifier = Modifier.width(Spacing.xxs))
                    Text(
                        text = "${marker.nombreUsuario} · ${marker.tiempoTexto}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // El lugar va en su propia línea y no pegado al tiempo: en esta
                // tarjeta la columna es angosta y "Jose · hace 2 h · Chapinero,
                // Bogotá" se cortaría casi siempre.
                if (marker.lugarTexto != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xxs))
                        Text(
                            text = marker.lugarTexto,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = marker.calificacion)
                    if (marker.likesCount > 0) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = marker.likesCount.toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "${marker.likesCount} me gusta"
                            }
                        )
                    }
                }

                if (marker.comentario.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = marker.comentario,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.testTag("reviewMarkerCardClose")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.reviews_map_close_card_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onAuthorClick(marker.userId) },
                modifier = Modifier.testTag("reviewMarkerCardAuthor")
            ) {
                Text(stringResource(R.string.reviews_map_view_profile))
            }
            TextButton(
                onClick = { onCardClick(marker.vehicleId) },
                modifier = Modifier.testTag("reviewMarkerCardVehicle")
            ) {
                Text(stringResource(R.string.reviews_map_view_vehicle))
            }
        }
    }
}

@Composable
private fun VehiclePhoto(imageUrl: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.reviews_map_vehicle_photo_cd, name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * Las estrellas se agrupan en un solo nodo de accesibilidad ("4 de 5
 * estrellas") en vez de anunciar cuatro iconos sueltos sin descripción.
 */
@Composable
private fun RatingStars(rating: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.reviews_map_rating_cd, rating)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(rating) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AuthorAvatar(avatarUrl: String?, name: String, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.reviews_map_avatar_cd, name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ReviewMarkerCardPreview() {
    MotorMatesTheme {
        ReviewMarkerCard(
            marker = ReviewMapMarkerUi(
                id = "r1",
                userId = "uid-jose",
                vehicleId = "1",
                lat = 4.6280,
                lng = -74.0643,
                nombreUsuario = "Jose Perfil",
                avatarUrl = null,
                vehicleName = "Porsche 911 GT3",
                tiempoTexto = "hace 2 h",
                calificacion = 4,
                comentario = "El sonido del motor es otro nivel. Se siente pegado al piso.",
                lugarTexto = "Chapinero, Bogotá",
                likesCount = 12
            )
        )
    }
}
