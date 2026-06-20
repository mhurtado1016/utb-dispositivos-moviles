package com.example.gestordetareas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla del formulario para agregar una nueva tarea.
 *
 * El estado de [title] y [description] se gestiona desde el padre para evitar
 * pérdidas de texto por recomposición. La ubicación se muestra como un badge
 * cuando el usuario la ha seleccionado desde el mapa.
 *
 * @param modifier Modifier externo con el padding de sistema.
 * @param title Valor actual del campo nombre.
 * @param description Valor actual del campo descripción.
 * @param latitude Latitud seleccionada en el mapa (null si no se seleccionó).
 * @param longitude Longitud seleccionada en el mapa (null si no se seleccionó).
 * @param onTitleChange Callback al cambiar el campo nombre.
 * @param onDescriptionChange Callback al cambiar el campo descripción.
 * @param onPickLocation Callback invocado al presionar "Agregar ubicación" → lanza MapActivity.
 * @param onClearLocation Callback para eliminar la ubicación seleccionada.
 * @param onSave Callback invocado con la [Task] construida al presionar "Guardar".
 * @param onBack Callback invocado al presionar "Volver" sin guardar.
 */
@Composable
fun FormScreen(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    latitude: Double? = null,
    longitude: Double? = null,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickLocation: () -> Unit,
    onClearLocation: () -> Unit,
    onSave: (Task) -> Unit,
    onBack: () -> Unit
) {
    var showTitleError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = "Agregar Nueva Tarea",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campo de nombre
        Text(
            text = "Nombre de la tarea",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = {
                onTitleChange(it)
                showTitleError = false
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Escribe el nombre aquí") },
            isError = showTitleError,
            supportingText = {
                if (showTitleError) Text("Ingresa el nombre de la tarea")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Campo de descripción
        Text(
            text = "Descripción de la tarea",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = { Text("Describe la tarea") },
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Sección de ubicación
        Text(
            text = "Ubicación (opcional)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (latitude != null && longitude != null) {
            // Badge con las coordenadas seleccionadas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación seleccionada",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ubicación seleccionada",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Lat: %.5f, Lng: %.5f".format(latitude, longitude),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = onClearLocation) {
                        Text("Quitar", fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Botón para abrir el mapa
            OutlinedButton(
                onClick = onPickLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar ubicación")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Botones de acción
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        showTitleError = true
                        return@Button
                    }
                    onSave(
                        Task(
                            title       = title.trim(),
                            description = description.trim(),
                            latitude    = latitude,
                            longitude   = longitude
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar")
            }
        }
    }
}
