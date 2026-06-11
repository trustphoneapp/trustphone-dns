package com.yashwanthsurabhi.shielddns.filter

sealed class BlockDecision {
    object Allow : BlockDecision()
    data class Block(val listName: String) : BlockDecision()
}
