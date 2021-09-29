package com.aeturnum.test.audiostreamer.sockets
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

interface AudioStreamerService {
    @Receive
    fun observeWebSocket(): Flow<WebSocket.Event>

    @Send
    fun sendAudio(audio: ByteArray)

    @Send
    fun subscribe(text: String)

    @Receive
    fun observeAudio(): Flow<ByteArray>

    @Receive
    fun observeText(): Flow<String>
}
