package com.baothanhbin.game2d.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AlertDialog(
) {
    AlertDialog(
        onDismissRequest = { showExitConfirmation = false },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Text("Warning")
            }
        },
        text = {
            Text("Are you sure you want to return to the menu? Your game progress will not be saved.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showExitConfirmation = false
                    onBackToSplash()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF5722)
                )
            ) {
                Text("Có")
            }
        },
        dismissButton = {
            TextButton(onClick = { showExitConfirmation = false }) {
                Text("Không")
            }
        }
    )
}