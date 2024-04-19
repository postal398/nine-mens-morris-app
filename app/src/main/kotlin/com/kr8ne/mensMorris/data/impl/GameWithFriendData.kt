package com.kr8ne.mensMorris.data.impl

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.kr8ne.mensMorris.common.gameBoard.GameBoard
import com.kr8ne.mensMorris.common.gameBoard.utils.CacheUtils
import com.kr8ne.mensMorris.common.gameBoard.utils.gameStartPosition
import com.kr8ne.mensMorris.data.DataModel
import com.kr8ne.mensMorris.data.GameBoardInterface

/**
 * data for game with friend screen
 */
class GameWithFriendData(override val viewModel: ViewModel, navController: NavHostController) :
    DataModel, GameBoardInterface {
    override val gameBoard = GameBoard(
        pos = mutableStateOf(gameStartPosition),
        onClick = { index, func -> func(index) },
        onUndo = {},
        navController = navController
    )

    override suspend fun invokeBackend() {
        CacheUtils.resetCachedPositions()
    }
}
