package com.kr8ne.mensMorris.ui.interfaces

import androidx.compose.runtime.Composable
import com.kr8ne.mensMorris.viewModel.interfaces.ViewModelI

/**
 * creates interface for screen creation
 */
interface ScreenModel {

    /**
     * invokes render part
     * launched on the main thread
     */
    @Composable
    fun InvokeRender()

    /**
     * viewModel attached to the screen
     */
    val viewModel: ViewModelI
        get() {
            TODO("Not yet implemented")
        }
}
