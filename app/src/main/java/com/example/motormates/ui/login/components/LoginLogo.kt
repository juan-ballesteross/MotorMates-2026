package com.example.motormates.ui.login.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.motormates.R

/**
 * ContentScale.Crop escala la imagen para llenar el ancho y recorta
 * el sobrante vertical (el espacio en blanco arriba/abajo del PNG),
 * logrando que el logo se vea más grande dentro del mismo contenedor.
 */
@Composable
fun LoginLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_motormates),
        contentDescription = stringResource(R.string.login_logo_content_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(160.dp)
            .height(70.dp)
    )
}
