package com.tps.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tps.data.remote.UserFacingApiError
import com.tps.data.remote.sensitiveFieldErrorDialogText

@Composable
fun FieldErrorDialog(
    error: UserFacingApiError?,
    onDismiss: () -> Unit
) {
    if (error == null) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("内容需要修改") },
        text = { Text(sensitiveFieldErrorDialogText(error)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}
