package com.aeturnum.test.audiostreamer.main
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeturnum.test.audiostreamer.sockets.AudioStreamerService
import com.aeturnum.test.audiostreamer.sockets.models.Subscribe
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    service: AudioStreamerService,
) : ViewModel() {
    private val _audioData = MutableLiveData<ByteArray?>()
    val audioDataReceived: LiveData<ByteArray?> = _audioData


    init {
        service.observeWebSocket()
            .flowOn(Dispatchers.IO)
            .onEach { event ->
                if (event !is WebSocket.Event.OnMessageReceived) {
                    Timber.d("Event = ${event::class.java.simpleName}")
                }

                if (event is WebSocket.Event.OnConnectionOpened<*>) {
                    service.subscribe(
                        Subscribe(
                            itemType = "Audio type",
                            channels = listOf("Admin")
                        )
                    )
                }
            }
            .launchIn(viewModelScope)

        service.observeAudio()
            .flowOn(Dispatchers.IO)
            .onEach {
                _audioData.postValue(it)
            }
            .launchIn(viewModelScope)
    }
}
