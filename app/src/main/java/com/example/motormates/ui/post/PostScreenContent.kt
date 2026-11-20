package com.example.motormates.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motormates.data.model.VEHICLE_CATEGORIES
import com.example.motormates.data.model.VEHICLE_TRACTIONS
import com.example.motormates.data.model.VehicleFormField
import com.example.motormates.data.model.VehicleFormInput
import com.example.motormates.ui.post.components.ChoiceChipsRow
import com.example.motormates.ui.post.components.PostSectionTitle
import com.example.motormates.ui.post.components.PostTextField
import com.example.motormates.ui.post.components.VehiclePhotoPicker
import com.example.motormates.ui.theme.MotorMatesTheme
import com.example.motormates.ui.theme.Radius
import com.example.motormates.ui.theme.Spacing

/** Stateless: el formulario completo llega del ViewModel (state hoisting). */
@Composable
fun PostScreenContent(
    form: VehicleFormInput,
    photo: Any?,
    errors: Map<VehicleFormField, String>,
    isPublishing: Boolean,
    canPublish: Boolean,
    errorMessage: String?,
    onPickPhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onPotenciaChange: (String) -> Unit,
    onAceleracionChange: (String) -> Unit,
    onVelocidadChange: (String) -> Unit,
    onTraccionSelect: (String) -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    isDeleting: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // La app es edge-to-edge: sin esto el teclado tapa el formulario.
            // Con imePadding el área visible se encoge al abrir el teclado y el
            // campo enfocado se desplaza solo para quedar a la vista.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter)
            .testTag("postVehicleForm")
    ) {
        Text(
            text = if (isEditing) {
                "Los cambios se verán en tu garaje, en el detalle del carro y en las comparaciones."
            } else {
                "Publica tu carro: quedará en tu garaje y otros podrán compararlo con el suyo."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        VehiclePhotoPicker(
            photo = photo,
            onPickClick = onPickPhotoClick,
            onRemoveClick = onRemovePhotoClick
        )

        PostSectionTitle("Datos del vehículo")
        PostTextField(
            value = form.brand,
            onValueChange = onBrandChange,
            label = "Marca",
            placeholder = "Ej. Mazda",
            error = errors[VehicleFormField.BRAND],
            modifier = Modifier.testTag("postBrandField")
        )
        PostTextField(
            value = form.model,
            onValueChange = onModelChange,
            label = "Modelo",
            placeholder = "Ej. MX-5",
            error = errors[VehicleFormField.MODEL],
            modifier = Modifier.testTag("postModelField")
        )
        PostTextField(
            value = form.year,
            onValueChange = onYearChange,
            label = "Año",
            placeholder = "Ej. 2020",
            keyboardType = KeyboardType.Number,
            error = errors[VehicleFormField.YEAR],
            modifier = Modifier.testTag("postYearField")
        )

        PostSectionTitle("Categoría")
        ChoiceChipsRow(
            options = VEHICLE_CATEGORIES,
            selected = form.category,
            onSelect = onCategorySelect,
            error = errors[VehicleFormField.CATEGORY],
            testTagPrefix = "postCategory"
        )

        PostSectionTitle("Especificaciones (opcional)")
        Text(
            text = "Si las llenas, tu carro se puede comparar en potencia, aceleración y velocidad.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PostTextField(
                value = form.potenciaHp,
                onValueChange = onPotenciaChange,
                label = "Potencia",
                suffix = "hp",
                keyboardType = KeyboardType.Number,
                error = errors[VehicleFormField.POTENCIA],
                modifier = Modifier.weight(1f)
            )
            PostTextField(
                value = form.aceleracionS,
                onValueChange = onAceleracionChange,
                label = "0-100",
                suffix = "s",
                keyboardType = KeyboardType.Decimal,
                error = errors[VehicleFormField.ACELERACION],
                modifier = Modifier.weight(1f)
            )
        }
        PostTextField(
            value = form.velocidadKmh,
            onValueChange = onVelocidadChange,
            label = "Velocidad máxima",
            suffix = "km/h",
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            error = errors[VehicleFormField.VELOCIDAD]
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(text = "Tracción", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(Spacing.xxs))
        ChoiceChipsRow(
            options = VEHICLE_TRACTIONS,
            selected = form.traccion,
            onSelect = onTraccionSelect,
            testTagPrefix = "postTraction"
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(
            onClick = onPublishClick,
            enabled = canPublish,
            shape = RoundedCornerShape(Radius.md),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("postPublishButton")
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = if (isEditing) "Guardar cambios" else "Publicar vehículo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !isDeleting,
                shape = RoundedCornerShape(Radius.md),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("postDeleteButton")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(text = "Eliminar vehículo", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun PostScreenContentPreview() {
    MotorMatesTheme {
        PostScreenContent(
            form = VehicleFormInput(brand = "Mazda", model = "MX-5", year = "2020", category = "Deportivo"),
            photo = null,
            errors = mapOf(VehicleFormField.POTENCIA to "Entre 1 y 2000 hp"),
            isPublishing = false,
            canPublish = true,
            errorMessage = null,
            onPickPhotoClick = {},
            onRemovePhotoClick = {},
            onBrandChange = {},
            onModelChange = {},
            onYearChange = {},
            onCategorySelect = {},
            onPotenciaChange = {},
            onAceleracionChange = {},
            onVelocidadChange = {},
            onTraccionSelect = {},
            onPublishClick = {}
        )
    }
}
