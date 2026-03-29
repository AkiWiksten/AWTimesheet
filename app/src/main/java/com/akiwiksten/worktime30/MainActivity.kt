package com.akiwiksten.worktime30

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akiwiksten.worktime30.ui.theme.WorkTime30Theme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTime30Theme {
                WorkTime30App()
            }
        }
    }
}

// Navigation routes
sealed class Screen(val route: String) {
    object Tab1 : Screen("tab1")
    object Tab2 : Screen("tab2")
    object Tab3 : Screen("tab3")
    object Detail : Screen("detail/{id}") {
        fun create(id: String) = "detail/$id"
    }
}

// App composable with bottom navigation
@Composable
fun WorkTime30App() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Tab1, Screen.Tab2, Screen.Tab3).forEach { screen ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(screen.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(screen.route) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Tab1.route, modifier = Modifier.padding(padding)) {

            composable(Screen.Tab1.route) {
                TabScreen(
                    "Tab 1",
                    onItemClick = { id -> navController.navigate(Screen.Detail.create(id)) }
                )
            }

            composable(Screen.Tab2.route) {
                TabScreen(
                    "Tab 2",
                    onItemClick = { id -> navController.navigate(Screen.Detail.create(id)) }
                )
            }

            composable(Screen.Tab3.route) {
                TabScreen(
                    "Tab 3",
                    onItemClick = { id -> navController.navigate(Screen.Detail.create(id)) }
                )
            }

            composable(Screen.Detail.route) { backStack ->
                val id = backStack.arguments?.getString("id") ?: ""
                DetailScreen(id)
            }
        }
    }
}

// Shared ViewModel (Hilt)
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    fun getItems(): List<String> = List(10) { "Item $it" }
}

// Tab screen
@Composable
fun TabScreen(title: String, onItemClick: (String) -> Unit, viewModel: MainViewModel = hiltViewModel()) {
    val items = viewModel.getItems()

    Column {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        LazyColumn {
            items(items) { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(16.dp)
                )
            }
        }
    }
}

// Detail screen
@Composable
fun DetailScreen(id: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Detail for $id")
    }
}
