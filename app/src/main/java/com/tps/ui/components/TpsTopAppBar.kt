package com.tps.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.RowScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TpsTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon ?: {},
        actions = actions ?: {},
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTpsTopAppBar() {
    TpsTopAppBar(
        title = "首页",
        navigationIcon = {
            IconButton(onClick = { /* Handle back click */ }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* Handle search click */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
    )
}
