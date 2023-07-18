package com.nosoproject.nosowallet.model

data class NosoConfig (
    var state:String = "Normal",
    var seedNodes:List<NodeInfo>,
    var demoAddress:List<String>,
    var poolList:List<NodeInfo>
)