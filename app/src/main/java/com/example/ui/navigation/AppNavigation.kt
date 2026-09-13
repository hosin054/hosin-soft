package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.accounting.ClosingAccountsScreen
import com.example.ui.screens.accounting.JournalVouchersScreen
import com.example.ui.screens.cash.CashScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.importexport.ImportExportScreen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.lock.LockScreen
import com.example.ui.screens.parties.CustomerDetailScreen
import com.example.ui.screens.parties.PartiesScreen
import com.example.ui.screens.parties.SupplierDetailScreen
import com.example.ui.screens.pos.InvoiceDetailScreen
import com.example.ui.screens.pos.PosScreen
import com.example.ui.screens.products.ProductFormScreen
import com.example.ui.screens.products.ProductsScreen
import com.example.ui.screens.purchases.PurchaseFormScreen
import com.example.ui.screens.purchases.PurchasesScreen
import com.example.ui.screens.quotations.QuotationsScreen
import com.example.ui.screens.labels.BarcodeLabelScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.AuditLogsScreen
import com.example.ui.screens.settings.CurrenciesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.setup.SetupScreen
import com.example.ui.screens.users.UsersScreen
import com.example.ui.screens.vouchers.VouchersScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, "الرئيسية", Icons.Default.Dashboard)
    object Pos : BottomNavItem(Screen.Pos.route, "بيع للزبون", Icons.Default.PointOfSale)
    object Products : BottomNavItem(Screen.Products.route, "المنتجات", Icons.Default.Inventory2)
    object Parties : BottomNavItem(Screen.Parties.route, "الحسابات", Icons.Default.People)
    object Purchases : BottomNavItem(Screen.Purchases.route, "مشتريات المحل", Icons.Default.ShoppingCart)
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Determine initial destination
    val startDestination = remember(settings, isUnlocked) {
        if (settings == null || !settings!!.isConfigured) {
            Screen.Setup.route
        } else if (!settings!!.securityPin.isNullOrBlank() && !isUnlocked) {
            Screen.Lock.route
        } else {
            Screen.Dashboard.route
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Pos,
        BottomNavItem.Products,
        BottomNavItem.Parties,
        BottomNavItem.Purchases
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    @Composable
    fun AppNavContent(modifier: Modifier = Modifier) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {
            composable(Screen.Setup.route) {
                SetupScreen(
                    viewModel = viewModel,
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Lock.route) {
                LockScreen(
                    viewModel = viewModel,
                    onUnlocked = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }

            composable(Screen.Pos.route) {
                PosScreen(
                    viewModel = viewModel,
                    onNavigateToInvoice = { invoiceId ->
                        navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId))
                    }
                )
            }

            composable(Screen.Products.route) {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToForm = { productId ->
                        navController.navigate(Screen.ProductForm.createRoute(productId))
                    },
                    onNavigateToInventory = {
                        navController.navigate(Screen.Inventory.route)
                    },
                    onNavigateToBarcodeLabels = { productId ->
                        navController.navigate(Screen.BarcodeLabels.createRoute(productId))
                    }
                )
            }

            composable(
                route = Screen.ProductForm.route,
                arguments = listOf(navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = 0L
                })
            ) { backStack ->
                val productId = backStack.arguments?.getLong("productId") ?: 0L
                ProductFormScreen(
                    productId = productId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Inventory.route) {
                InventoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Parties.route) {
                PartiesScreen(
                    viewModel = viewModel,
                    onNavigateToCustomer = { id ->
                        navController.navigate(Screen.CustomerDetail.createRoute(id))
                    },
                    onNavigateToSupplier = { id ->
                        navController.navigate(Screen.SupplierDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.CustomerDetail.route,
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStack ->
                val customerId = backStack.arguments?.getLong("customerId") ?: 0L
                CustomerDetailScreen(
                    customerId = customerId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.SupplierDetail.route,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType })
            ) { backStack ->
                val supplierId = backStack.arguments?.getLong("supplierId") ?: 0L
                SupplierDetailScreen(
                    supplierId = supplierId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Purchases.route) {
                PurchasesScreen(
                    viewModel = viewModel,
                    onNavigateToNewPurchase = { navController.navigate(Screen.PurchaseForm.route) },
                    onNavigateToInvoice = { id ->
                        navController.navigate(Screen.InvoiceDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.PurchaseForm.route) {
                PurchaseFormScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onInvoiceCreated = { id ->
                        navController.navigate(Screen.InvoiceDetail.createRoute(id)) {
                            popUpTo(Screen.Purchases.route)
                        }
                    }
                )
            }

            composable(Screen.Vouchers.route) {
                VouchersScreen(
                    viewModel = viewModel,
                    onNavigateToJournal = { navController.navigate(Screen.JournalVouchers.route) }
                )
            }

            composable(Screen.JournalVouchers.route) {
                JournalVouchersScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ClosingAccounts.route) {
                ClosingAccountsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Expenses.route) {
                ExpensesScreen(viewModel = viewModel)
            }

            composable(Screen.Cash.route) {
                CashScreen(viewModel = viewModel)
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateToClosingAccounts = { navController.navigate(Screen.ClosingAccounts.route) },
                    onNavigateToJournal = { navController.navigate(Screen.JournalVouchers.route) }
                )
            }

            composable(Screen.ImportExport.route) {
                ImportExportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateTo = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AuditLogs.route) {
                AuditLogsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Currencies.route) {
                CurrenciesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Users.route) {
                UsersScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Quotations.route) {
                QuotationsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPos = { navController.navigate(Screen.Pos.route) }
                )
            }

            composable(
                route = Screen.BarcodeLabels.route,
                arguments = listOf(navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = 0L
                })
            ) { backStack ->
                val productId = backStack.arguments?.getLong("productId") ?: 0L
                BarcodeLabelScreen(
                    viewModel = viewModel,
                    initialProductId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.InvoiceDetail.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStack ->
                val invoiceId = backStack.arguments?.getLong("invoiceId") ?: 0L
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet && showBottomBar) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "حسين سوفت",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) { innerPadding ->
                    AppNavContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = {
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                AppNavContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
