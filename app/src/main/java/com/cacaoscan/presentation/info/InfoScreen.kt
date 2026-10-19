package com.cacaoscan.presentation.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cacaoscan.presentation.common.CacaoScreen

/**
 * Informacion sobre la monilia (RF07) y guia de uso (RF08).
 *
 * Todo el contenido es local: no hay descargas ni enlaces externos, porque la
 * aplicacion debe ser plenamente util sin conexion (RNF02).
 */
@Composable
fun InfoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember { listOf("La monilia", "Usar la app") }
    val sections = if (selectedTab == 0) InfoContent.aboutMonilia else InfoContent.howToUse

    CacaoScreen(
        title = "Informacion",
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    ) { contentModifier ->
        Column(modifier = contentModifier.fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(text = title, style = MaterialTheme.typography.titleMedium)
                        },
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 18.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = sections, key = { it.title }) { section ->
                    InfoCard(section = section)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(section: InfoSection, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = section.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            section.paragraphs.forEachIndexed { index, paragraph ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
