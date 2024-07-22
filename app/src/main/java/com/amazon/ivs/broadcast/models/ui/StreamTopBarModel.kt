package com.amazon.ivs.broadcast.models.ui

import com.amazon.ivs.broadcast.R
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.amazonaws.ivs.broadcast.BroadcastSession.State.CONNECTED
import com.amazonaws.ivs.broadcast.BroadcastSession.State.DISCONNECTED

data class StreamTopBarModel(
    val streamStatus: BroadcastSession.State = DISCONNECTED,
    val pillBackground: Int = R.drawable.bg_offline_pill
) {
    val isStreamOnline: Boolean = streamStatus == CONNECTED
}
