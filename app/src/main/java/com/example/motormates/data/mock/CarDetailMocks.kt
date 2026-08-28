package com.example.motormates.data.mock

import com.example.motormates.R
import com.example.motormates.data.model.CarDetailUi
import com.example.motormates.data.model.ReviewUi

object CarDetailMocks {
    val mockCarDetail = CarDetailUi(
        marca = "PORSCHE",
        modelo = "911 GT3",
        imagenResId = VehicleImages.PORSCHE_911_GT3,
        calificacion = 4.9f,
        numeroResenas = 312,
        potencia = "510 hp",
        aceleracion = "3.4 s",
        velocidadMaxima = "318 km/h",
        traccion = "RWD"
    )

    val mockReviews = listOf(
        ReviewUi(
            id = 1,
            nombreUsuario = "Sofía Reyes",
            avatarResId = R.drawable.user1,
            tiempoTexto = "hace 3 días",
            calificacion = 5,
            comentario = "El mejor GT3 que he probado. El escape en modo Sport es adictivo."
        ),
        ReviewUi(
            id = 2,
            nombreUsuario = "Iván Pérez",
            avatarResId = R.drawable.user2,
            tiempoTexto = "hace 5 días",
            calificacion = 5,
            comentario = "El mejor GT3 que he probado. El escape en modo Sport es adictivo."
        )
    )
}
