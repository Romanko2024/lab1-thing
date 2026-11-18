package com.example.lab1_thing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.lab1_thing.ui.theme.Lab1thingTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.HorizontalList
import com.example.lab1_thing.model.MyListable
import com.example.lab1_thing.model.Track
import com.example.lab1_thing.ui.components.ArtistCard
import com.example.lab1_thing.ui.components.ArtistHorizontalCard
import com.example.lab1_thing.ui.components.TrackCard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab1thingTheme {
                Lab1thingApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun Lab1thingApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(painterResource(it.icon), contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val name = stringResource(R.string.greeting_name)
            when (currentDestination) {
                AppDestinations.HOME -> HomeNavDisplay(
                    modifier = Modifier.padding(innerPadding),
                    name = name
                )
                AppDestinations.SETTINGS -> SettingsRoute(
                    modifier = Modifier.padding(innerPadding),
                    name = name
                )
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: Int) {
    HOME("Головна", R.drawable.ic_home),
    SETTINGS("Налаштування", R.drawable.ic_account_box);
}

@Composable
fun HomeNavDisplay(
    modifier: Modifier = Modifier,
    name: String,
    viewModel: HomeViewModel = viewModel()
) {
    val labelText by viewModel.labelText.collectAsState()
//  val backStack = rememberNavBackStack(HomeScreen.Main)
    val backStack by viewModel.homeBackStack.collectAsState()
    val catalogItems by viewModel.catalogItems.collectAsState()

    NavDisplay(
        backStack = backStack,
        modifier = modifier.fillMaxSize(),
//      onBack = { backStack.removeLastOrNull() },
        onBack = { viewModel.navigateBack() },
        entryProvider = entryProvider {
            entry<HomeScreen.Main> {
                HomeMainContent(
                    labelText = labelText,
                    onUpdateText = viewModel::updateText,
                    name = name,
                    items = catalogItems,
                    onNavigateToDetails = {
//                      backStack.add(HomeScreen.Details)
                        viewModel.navigateTo(HomeScreen.Details)
                    }
                )
            }

            entry<HomeScreen.Details> {
                HomeDetailsContent(
                    onBack = {
//                      backStack.removeLastOrNull()
                        viewModel.navigateBack()
                    }
                )
            }
        }
    )
}

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    name: String,
    viewModel: SettingsViewModel = viewModel()
) {
    val labelText by viewModel.labelText.collectAsState()
    SettingsScreen(
        labelText = labelText,
        onUpdateText = viewModel::updateText,
        name = name,
        modifier = modifier
    )
}

@Composable
fun HomeMainContent(
    labelText: String,
    onUpdateText: (String) -> Unit,
    name: String,
    items: List<MyListable>,
    onNavigateToDetails: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello, $name", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = labelText, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onUpdateText("Текст змінено на Головній!") },
            modifier = Modifier.fillMaxWidth(0.8f).height(64.dp)
        ) { Text("Змінити текст") }

        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = items,
                key = { item ->
                    "${item.id}_${when (item) {
                        is Artist -> item.name
                        is Track -> item.title
                        is HorizontalList -> "h_${item.id}"
                    }}"
                }
            ) { item ->
                when (item) {
                    is Artist -> ArtistCard(artist = item)
                    is Track -> TrackCard(track = item)
                    is HorizontalList -> HorizontalArtistsRow(horizontalList = item)
                }
            }
        }

        Button(
            onClick = onNavigateToDetails,
            modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 12.dp)
        ) { Text("Перейти далі") }
    }
}

@Composable
private fun HorizontalArtistsRow(
    horizontalList: HorizontalList,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = horizontalList.artistList,
            key = { artist -> "h_${artist.id}_${artist.name}" }
        ) { artist ->
            ArtistHorizontalCard(artist = artist)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDetailsContent(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Деталі",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.outline_arrow_back_24),
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Це другий підекран", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Тут може бути будь-яка довільна інформація.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    labelText: String,
    onUpdateText: (String) -> Unit,
    name: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Hello, $name", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = labelText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onUpdateText("Налаштування змінено!") },
            modifier = Modifier.width(120.dp).height(40.dp)
        ) {
            Text("Змінити")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    Lab1thingTheme {
        Lab1thingApp()
    }
}
