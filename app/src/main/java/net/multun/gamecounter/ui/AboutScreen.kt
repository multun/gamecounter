package net.multun.gamecounter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import net.multun.gamecounter.BuildConfig
import net.multun.gamecounter.R
import net.multun.gamecounter.ui.main_menu.AppLogo

@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            GameCounterTopBar(stringResource(R.string.about), navController)
        }
    ) { contentPadding ->
        LibrariesContainer(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            header = {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    ) {
                        AppLogo(Modifier.size(100.dp))
                        Text("Version %s".format(BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.free_for_all_made_with_love))

                        Text(stringResource(R.string.source_code_is_available_under_the_terms_of_the_gplv3_license))
                    }
                }
            }
        )
    }
}