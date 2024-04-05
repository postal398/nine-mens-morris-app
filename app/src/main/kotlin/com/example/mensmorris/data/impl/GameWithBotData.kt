package com.example.mensmorris.data.impl

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import com.example.mensmorris.common.gameBoard.GameBoard
import com.example.mensmorris.common.utils.CacheUtils
import com.example.mensmorris.common.utils.GameUtils
import com.example.mensmorris.common.utils.defaultDispatcher
import com.example.mensmorris.data.DataModel
import com.example.mensmorris.data.GameBoardInterface
import com.example.mensmorris.model.impl.GameAnalyzeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * data for game with bot screen
 */
class GameWithBotData : DataModel, GameBoardInterface {
    override val gameBoard = MutableLiveData(
        GameBoard(pos = mutableStateOf(GameUtils.gameStartPosition),
            onClick = { index, func -> response(index, func) },
            onUndo = {})
    )
    private val analyze = GameAnalyzeViewModel(gameBoard.value!!.pos)

    /**
     * used for storing our analyze coroutine
     * gets force-stopped when no longer needed
     */
    private var solvingJob: Job? = null

    /**
     * performs needed actions after click
     * @param index index of the clicked element
     * @param func function that handles our click
     */
    private fun response(index: Int, func: (index: Int) -> Unit) {
        if (gameBoard.value!!.pos.value.pieceToMove) {
            func(index)
        }
    }

    override suspend fun invokeBackend() {
        CacheUtils.resetCachedPositions()
        launchBot()
    }

    override fun clearTheScene() {
        solvingJob?.cancel()
        CacheUtils.position = gameBoard.value!!.pos.value
    }

    /**
     * launches bot actions against player
     */
    private suspend fun launchBot() {
        CoroutineScope(defaultDispatcher).async {
            while (true) {
                if (!gameBoard.value!!.pos.value.pieceToMove
                    && gameBoard.value!!.pos.value.gameState() != GameUtils.GameState.End
                ) {
                    analyze.data.startAnalyze()
                    gameBoard.value!!.gameClickHandler.processMove(analyze.data.solveResult.value!!.last())
                    CacheUtils.resetCachedPositions()
                }
                delay(500)
            }
        }
    }
}
