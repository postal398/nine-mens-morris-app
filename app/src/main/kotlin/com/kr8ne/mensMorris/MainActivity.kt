package com.kr8ne.mensMorris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kr8ne.mensMorris.common.utils.positionToNuke
import com.kr8ne.mensMorris.model.impl.GameEndViewModel
import com.kr8ne.mensMorris.model.impl.GameWithBotViewModel
import com.kr8ne.mensMorris.model.impl.GameWithFriendViewModel
import com.kr8ne.mensMorris.model.impl.WelcomeViewModel

/**
 * shows how thick our pieces & board will be
 */
val BUTTON_WIDTH = 35.dp

/**
 * represents current activity
 * used for switching screens
 */
lateinit var activity: ComponentActivity

/**
 * activity our app is launched from
 */
class MainActivity : ComponentActivity() {

    /**
     * nav controller for this activity
     */
    lateinit var navController: NavHostController

    /**
     * we initialize all important stuff here
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = WELCOME_SCREEN) {
                composable(WELCOME_SCREEN) {
                    WelcomeViewModel(navController).Invoke()
                }
                composable(GAME_WITH_BOT_SCREEN) {
                    GameWithBotViewModel(navController).Invoke()
                }
                composable(GAME_WITH_FRIEND_SCREEN) {
                    GameWithFriendViewModel(navController).Invoke()
                }
                composable(GAME_END_SCREEN) {
                    GameEndViewModel(positionToNuke, navController).Invoke()
                }
            }
        }
    }
}
