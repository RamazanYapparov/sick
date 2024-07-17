package com.sick.service.game.create

import com.sick.model.Game
import com.sick.model.Package

class GameCreateService {
    fun execute(pack: Package) = Game(pack)
}