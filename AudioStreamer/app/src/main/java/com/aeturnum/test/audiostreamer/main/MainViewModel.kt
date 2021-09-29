package com.aeturnum.test.audiostreamer.main
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeturnum.test.audiostreamer.sockets.AudioStreamerService
import com.tinder.scarlet.WebSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    var service: AudioStreamerService,
) : ViewModel() {

    private val _audioData = MutableLiveData<ByteArray?>()
    val audioDataReceived: LiveData<ByteArray?> = _audioData

    private val _textData = MutableLiveData<String?>()
    val textDataReceived: LiveData<String?> = _textData

    //fixme --- (connection unstable issue)
    fun sendAudioData(data:ByteArray){
        viewModelScope.launch(Dispatchers.IO) {
            service.sendAudio(data)
        }
    }

    init {
        service.observeWebSocket()
            .flowOn(Dispatchers.IO)
            .onEach { event ->
                if (event !is WebSocket.Event.OnMessageReceived) {
                    Timber.d("Event = ${event::class.java.simpleName}")
                }

                if (event is WebSocket.Event.OnConnectionOpened<*>) {
                    service.subscribe("Subscribe from mobile client  ")
                }
            }.launchIn(viewModelScope)


        service.observeAudio()
            .flowOn(Dispatchers.IO)
            .onEach {
                _audioData.postValue(it)
            }
            .launchIn(viewModelScope)

        service.observeText()
            .flowOn(Dispatchers.IO)
            .onEach {
                _textData.postValue(it)
            }
            .launchIn(viewModelScope)

    }
}
