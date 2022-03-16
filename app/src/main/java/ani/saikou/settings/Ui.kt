package ani.saikou.settings

import java.io.Serializable

data class UserInterface(
    var darkMode:Boolean?=null,

    var immersiveMode:Boolean = false,

    var bannerAnimations :Boolean = true,
    var animationSpeed :Double = 1.0,
    var layoutAnimations :Boolean = true,

    var homeLayoutShow : ArrayList<Boolean> = arrayListOf(true,false,true,false,true)
) : Serializable