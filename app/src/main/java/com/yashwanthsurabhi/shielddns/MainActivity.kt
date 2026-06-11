package com.yashwanthsurabhi.shielddns

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yashwanthsurabhi.shielddns.ui.ShieldViewModel
import com.yashwanthsurabhi.shielddns.ui.components.ShieldScaffold
import com.yashwanthsurabhi.shielddns.ui.navigation.NavRoutes
import com.yashwanthsurabhi.shielddns.ui.screens.AboutScreen
import com.yashwanthsurabhi.shielddns.ui.screens.ActivityScreen
import com.yashwanthsurabhi.shielddns.ui.screens.AllowlistScreen
import com.yashwanthsurabhi.shielddns.ui.screens.AppsScreen
import com.yashwanthsurabhi.shielddns.ui.screens.HomeScreen
import com.yashwanthsurabhi.shielddns.ui.screens.ListsScreen
import com.yashwanthsurabhi.shielddns.ui.screens.MoreScreen
import com.yashwanthsurabhi.shielddns.ui.screens.RulesScreen
import com.yashwanthsurabhi.shielddns.ui.screens.SettingsScreen
import com.yashwanthsurabhi.shielddns.ui.screens.StatsScreen
import com.yashwanthsurabhi.shielddns.ui.theme.ShieldDnsTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHostState

class MainActivity : ComponentActivity() {

    private val viewModel: ShieldViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* continue — VPN notification still attempts to show */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            if (intent?.getBooleanExtra("request_vpn", false) == true) {
                viewModel.toggleProtection(true)
            }
        }

        lifecycleScope.launch {
            viewModel.vpnPermissionIntent.collect { intent ->
                vpnPermissionLauncher.launch(intent)
            }
        }
        lifecycleScope.launch {
            viewModel.requestNotificationPermission.collect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            val blocked by viewModel.filteredBlocked.collectAsState()
            val search by viewModel.searchQuery.collectAsState()
            val apps by viewModel.installedApps.collectAsState()
            val rules by viewModel.appRules.collectAsState()
            val blocklistCount by viewModel.blocklistCount.collectAsState()
            val blocklistCategoryCounts by viewModel.blocklistCategoryCounts.collectAsState()
            val blocklistUpdateState by viewModel.blocklistUpdateState.collectAsState()
            val networkType by viewModel.networkType.collectAsState()
            val isPro by viewModel.isPro.collectAsState()
            val billingUiState by viewModel.billingUiState.collectAsState()

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(viewModel) {
                viewModel.userMessage.collect { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            ShieldDnsTheme(darkTheme = settings.darkTheme) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route ?: NavRoutes.HOME
                var exportJson by rememberSaveable { mutableStateOf("") }

                ShieldScaffold(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME,
                        modifier = padding,
                    ) {
                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                settings = settings,
                                networkType = networkType,
                                blocklistCount = blocklistCount,
                                onToggle = viewModel::toggleProtection,
                            )
                        }
                        composable(NavRoutes.LISTS) {
                            ListsScreen(
                                settings = settings,
                                isPro = isPro,
                                proPrice = billingUiState.proPrice,
                                blocklistCount = blocklistCount,
                                categoryCounts = blocklistCategoryCounts,
                                updateState = blocklistUpdateState,
                                onToggle = viewModel::setListEnabled,
                                onUpdate = viewModel::updateBlocklistsNow,
                                onCustomUrl = viewModel::setCustomUrl,
                                onUpgrade = { viewModel.purchasePro(this@MainActivity) },
                            )
                        }
                        composable(NavRoutes.ACTIVITY) {
                            ActivityScreen(
                                queries = blocked,
                                searchQuery = search,
                                onSearchChange = viewModel::setSearchQuery,
                            )
                        }
                        composable(NavRoutes.MORE) {
                            MoreScreen(
                                isPro = isPro,
                                proPrice = billingUiState.proPrice,
                                onNavigate = { route ->
                                    navController.navigate(route) { launchSingleTop = true }
                                },
                                onUpgrade = { viewModel.purchasePro(this@MainActivity) },
                            )
                        }
                        composable(NavRoutes.ALLOWLIST) {
                            AllowlistScreen(
                                allowlist = settings.allowlist,
                                customDeny = settings.customDeny,
                                onAllowlistChange = viewModel::setAllowlist,
                                onDenyChange = viewModel::setCustomDeny,
                            )
                        }
                        composable(NavRoutes.APPS) {
                            AppsScreen(
                                apps = apps,
                                rules = rules,
                                isPro = isPro,
                                onSetRule = viewModel::setAppRule,
                            )
                        }
                        composable(NavRoutes.RULES) {
                            RulesScreen(
                                settings = settings,
                                isPro = isPro,
                                scheduleLabel = viewModel.scheduleLabel(settings),
                                onScheduleToggle = { enabled ->
                                    viewModel.setSchedule(enabled, settings.scheduleStartMinutes, settings.scheduleEndMinutes)
                                },
                                onScheduleChange = { start, end ->
                                    viewModel.setSchedule(settings.scheduleEnabled, start, end)
                                },
                                onNetworkRules = viewModel::setNetworkRules,
                            )
                        }
                        composable(NavRoutes.STATS) {
                            StatsScreen(settings = settings)
                        }
                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                isPro = isPro,
                                proPrice = billingUiState.proPrice,
                                exportedJson = exportJson,
                                onUpstreamChange = viewModel::setUpstream,
                                onDohChange = viewModel::setUseDoh,
                                onBootChange = viewModel::setStartOnBoot,
                                onThemeChange = viewModel::setDarkTheme,
                                onExport = {
                                    viewModel.exportSettings().also { exportJson = it }
                                },
                                onImport = viewModel::importSettings,
                                onUnlockPro = { viewModel.purchasePro(this@MainActivity) },
                            )
                        }
                        composable(NavRoutes.ABOUT) {
                            AboutScreen()
                        }
                        composable(NavRoutes.RESOLVER_HEALTH) {
                            com.yashwanthsurabhi.shielddns.ui.screens.ResolverHealthScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.ADVANCED_ENGINE) {
                            com.yashwanthsurabhi.shielddns.ui.screens.AdvancedEngineScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.PRIVACY_CENTER) {
                            com.yashwanthsurabhi.shielddns.ui.screens.PrivacyCenterScreen(
                                settings = settings,
                                viewModel = viewModel,
                                onExportCsv = viewModel::exportLogsToCsv,
                            )
                        }
                        composable(NavRoutes.POLICY_CENTER) {
                            com.yashwanthsurabhi.shielddns.ui.screens.PolicyCenterScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.FAMILY_CENTER) {
                            com.yashwanthsurabhi.shielddns.ui.screens.FamilyCenterScreen(viewModel = viewModel)
                        }
                        composable(NavRoutes.THREAT_CENTER) {
                            com.yashwanthsurabhi.shielddns.ui.screens.ThreatCenterScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
