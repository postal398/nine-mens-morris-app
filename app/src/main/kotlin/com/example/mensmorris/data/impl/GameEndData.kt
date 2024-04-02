package com.example.mensmorris.data.impl

import com.example.mensmorris.data.DataModel
import com.example.mensmorris.utils.CacheUtils

class GameEndData : DataModel {
    override fun invokeBackend() {
        CacheUtils.moveHints.value = arrayListOf()
    }

    override fun clearTheScene() {
        TODO("Not yet implemented")
    }
}