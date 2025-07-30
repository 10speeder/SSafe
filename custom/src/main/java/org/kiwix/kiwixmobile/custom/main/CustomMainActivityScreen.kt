/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.custom.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.kiwix.kiwixmobile.core.main.DrawerMenuGroup
import org.kiwix.kiwixmobile.core.main.LeftDrawerMenu
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme

@Composable
fun CustomMainActivityScreen(
  navController: NavHostController,
  leftDrawerContent: List<DrawerMenuGroup>,
  topLevelDestinationsRoute: Set<String>,
  leftDrawerState: DrawerState,
  enableLeftDrawer: Boolean,
) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  KiwixTheme {
    ModalNavigationDrawer(
      drawerState = leftDrawerState,
      drawerContent = {
        Column(modifier = Modifier.fillMaxSize()) {
          LeftDrawerMenu(leftDrawerContent)
        }
      },
      gesturesEnabled = enableLeftDrawer &&
        currentRoute in topLevelDestinationsRoute &&
        // Fixing the webView scrolling is lagging when navigation gesture is enabled,
        // since navigation consumes the swipes event makes webView lagging.
        // However, on reader screen navigation drawer can be opened by clicking
        // on the hamburger button.
        (currentRoute != CustomDestination.Reader.route || leftDrawerState.isOpen)
    ) {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
      ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
          CustomNavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
