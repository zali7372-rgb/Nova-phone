package hu.nova.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import hu.nova.mobile.navigation.NovaNavGraph
import hu.nova.mobile.ui.theme.NovaTheme
import hu.nova.mobile.ui.NovaViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NovaApplication
        val viewModelFactory = NovaViewModelFactory(app)

        setContent {
            NovaRoot(viewModelFactory)
        }
    }
}

@Composable
private fun NovaRoot(viewModelFactory: NovaViewModelFactory) {
    NovaTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NovaNavGraph(
                navController = navController,
                viewModelFactory = viewModelFactory
            )
        }
    }
}
