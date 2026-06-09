package com.tps.ui.home

/**
 * 文件说明：首页模块界面，负责主入口与底部功能页的整合。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tps.ui.message.MessageListScreen
import com.tps.ui.order.MyOrdersScreen
import com.tps.ui.product.HomeProductListScreen
import com.tps.ui.profile.MyProfileScreen
import com.tps.ui.theme.MarketGreen
import com.tps.ui.theme.MarketOrange

@Composable
fun MainScreen(
    onNavigateToProductDetail: (Long) -> Unit, 
    onNavigateToChat: (Long) -> Unit, 
    onNavigateToPublish: () -> Unit, 
    onLogout: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToOrdersRole: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val navController = rememberNavController()
    val tabs = listOf(
        TabItem("首页", Icons.Default.Home, "home"),
        TabItem("消息", Icons.Default.Message, "messages"),
        TabItem("订单", Icons.Default.Receipt, "orders"),
        TabItem("我的", Icons.Default.Person, "me")
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPublish,
                containerColor = MarketOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(19.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "发布", modifier = Modifier.padding(4.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                containerColor = Color.White.copy(alpha = 0.94f),
                tonalElevation = 10.dp,
            ) {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentRoute == tab.route || (tab.route == "orders" && currentRoute?.startsWith("orders") == true),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MarketGreen,
                            selectedTextColor = MarketGreen,
                            indicatorColor = Color(0xFFE5F4EE),
                            unselectedIconColor = Color(0xFF6E7973),
                            unselectedTextColor = Color(0xFF6E7973)
                        ),
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "home",
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF5F7F6), Color(0xFFFAFBFA), Color(0xFFEEF2F0))
                    )
                )
                .padding(innerPadding)
        ) {
            composable("home") { HomeProductListScreen(onNavigateToProductDetail) }
            composable("messages") { MessageListScreen(onNavigateToChat) }
            composable(
                route = "orders?role={role}",
                arguments = listOf(androidx.navigation.navArgument("role") { type = androidx.navigation.NavType.StringType; nullable = true })
            ) { backStackEntry -> 
                MyOrdersScreen(initialRole = backStackEntry.arguments?.getString("role")) 
            }
            composable("orders") { MyOrdersScreen(initialRole = null) }
            composable("me") { 
                MyProfileScreen(
                    onLogout = onLogout,
                    onNavigateToMyProducts = onNavigateToMyProducts,
                    onNavigateToOrders = onNavigateToOrdersRole,
                    onNavigateToFavorites = onNavigateToFavorites,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToFeedback = onNavigateToFeedback
                ) 
            }
        }
    }
}

data class TabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)
