package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.steefwel28.consumoenergetico.R

/**
 * Botón (i) reutilizable que abre un diálogo breve con lenguaje cotidiano.
 * Autocontenido: mantiene su propio estado de diálogo.
 */
@Composable
fun InfoHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPixelHaptics()
    var show by rememberSaveable { mutableStateOf(false) }
    if (show) {
        PixelDialog(
            title = stringResource(R.string.common_info_title),
            text = text,
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = { show = false },
            onDismissRequest = { show = false },
        )
    }
    IconButton(
        onClick = {
            haptics.tap()
            show = true
        },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = stringResource(R.string.common_info_cd),
        )
    }
}
