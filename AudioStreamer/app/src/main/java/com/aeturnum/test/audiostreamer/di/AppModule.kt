package com.aeturnum.test.audiostreamer.di
import android.content.Context
import com.aeturnum.test.audiostreamer.AudioStreamerApplication
import com.aeturnum.test.audiostreamer.sockets.AudioStreamerService
import com.aeturnum.test.audiostreamer.sockets.FlowStreamAdapter
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.lifecycle.android.BuildConfig
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun providesApplication(@ApplicationContext context: Context): AudioStreamerApplication {
        return context as AudioStreamerApplication
    }

    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @Provides
    fun provideHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor()
            .setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            )

        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    @Provides
    fun provideScarlet(application: AudioStreamerApplication, client: OkHttpClient, moshi: Moshi): Scarlet {
        return Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory("wss://0a5f-112-134-175-111.ngrok.io"))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory(moshi))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory())
            .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
            .build()
    }

    @Provides
    fun provideAudioStreamerService(scarlet: Scarlet): AudioStreamerService {
        return scarlet.create()
    }
}
