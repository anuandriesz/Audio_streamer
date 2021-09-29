package com.aeturnum.test.audiostreamer.domain

import com.aeturnum.test.audiostreamer.sockets.AudioStreamerService
import javax.inject.Inject

/**
 * An interactor that calls the actual implementation of [MainViewModel for Audio Data handling](which is injected by DI)
 * it handles the response that returns data &
 * contains a list of actions, event steps
 */
class SendAudioDataUseCase @Inject constructor(private val service: AudioStreamerService)  {

    fun sendAudio(audioData: ByteArray) {
        service.sendAudio(audioData)
    }

}
