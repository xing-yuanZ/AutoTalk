package com.autotalk.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autotalk.app.LocalAppContainer
import com.autotalk.app.ui.screens.ConversationListScreen
import com.autotalk.app.ui.screens.ConversationSetupScreen
import com.autotalk.app.ui.screens.LiveSessionScreen
import com.autotalk.app.ui.screens.OnboardingScreen
import com.autotalk.app.ui.screens.SettingsScreen
import com.autotalk.app.ui.screens.StyleCoachScreen
import com.autotalk.app.ui.viewmodels.AppVMFactory
import com.autotalk.app.ui.viewmodels.ConversationListViewModel
import com.autotalk.app.ui.viewmodels.ConversationSetupViewModel
import com.autotalk.app.ui.viewmodels.LiveSessionViewModel
import com.autotalk.app.ui.viewmodels.OnboardingViewModel
import com.autotalk.app.ui.viewmodels.SettingsViewModel
import com.autotalk.app.ui.viewmodels.StyleCoachViewModel

/** 顶层路由常量。 */
object Routes {
    const val ONBOARDING = "onboarding"

    // 三个主 Tab（扁平结构，便于底栏统一控制）
    const val TAB_CONVERSATIONS = "tab_conversations"
    const val TAB_COACH = "tab_coach"
    const val TAB_SETTINGS = "tab_settings"

    const val SETUP = "setup"
    const val LIVE = "live/{conversationId}"
    fun live(id: String) = "live/$id"
}

private val tabRoutes = setOf(Routes.TAB_CONVERSATIONS, Routes.TAB_COACH, Routes.TAB_SETTINGS)

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppNavigation() {
    val container = LocalAppContainer.current
    val settings by container.settingsState.collectAsState()
    val navController = rememberNavController()

    val startDestination = if (settings.hasCompletedOnboarding) Routes.TAB_CONVERSATIONS else Routes.ONBOARDING

    val tabs = listOf(
        TabItem(Routes.TAB_CONVERSATIONS, "对话", Icons.Filled.BubbleChart),
        TabItem(Routes.TAB_COACH, "教练", Icons.Filled.Person),
        TabItem(Routes.TAB_SETTINGS, "设置", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val current = navController.currentBackStackEntry?.destination?.route
            if (current in tabRoutes) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                if (current != tab.route) {
                                    navController.navigate(tab.route) {
                                        // 在主 Tab 之间切换：回到栈底并保留状态。
                                        popUpTo(Routes.TAB_CONVERSATIONS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                val vm = viewModel<OnboardingViewModel>(factory = AppVMFactory.onboarding(container))
                OnboardingScreen(
                    vm = vm,
                    onDone = {
                        navController.navigate(Routes.TAB_CONVERSATIONS) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.TAB_CONVERSATIONS) {
                val vm = viewModel<ConversationListViewModel>(factory = AppVMFactory.list(container))
                ConversationListScreen(
                    vm = vm,
                    onNew = { navController.navigate(Routes.SETUP) },
                    onOpen = { id -> navController.navigate(Routes.live(id)) }
                )
            }

            composable(Routes.TAB_COACH) {
                val vm = viewModel<StyleCoachViewModel>(factory = AppVMFactory.coach(container))
                StyleCoachScreen(vm = vm)
            }

            composable(Routes.TAB_SETTINGS) {
                val vm = viewModel<SettingsViewModel>(factory = AppVMFactory.settings(container))
                SettingsScreen(vm = vm)
            }

            composable(Routes.SETUP) {
                val vm = viewModel<ConversationSetupViewModel>(factory = AppVMFactory.setup(container))
                ConversationSetupScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.navigate(Routes.live(id)) {
                            popUpTo(Routes.SETUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.LIVE,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { entry ->
                val conversationId = entry.arguments?.getString("conversationId").orEmpty()
                val vm = viewModel<LiveSessionViewModel>(factory = AppVMFactory.live(container, conversationId))
                LiveSessionScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
