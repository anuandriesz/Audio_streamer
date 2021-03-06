package com.aeturnum.test.audiostreamer.di
import android.content.Context
import com.aeturnum.test.audiostreamer.AudioStreamerApplication
import com.aeturnum.test.audiostreamer.sockets.AudioStreamerService
import com.aeturnum.test.audiostreamer.sockets.FlowStreamAdapter
import com.aeturnum.test.audiostreamer.utils.Constants.BASE_URL
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.lifecycle.android.BuildConfig
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Singleton
    @Provides
    fun providesApplication(@ApplicationContext context: Context): AudioStreamerApplication {
        return context as AudioStreamerApplication
    }
    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor()
            .setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            )

        return OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()
    }
    @Singleton
    @Provides
    fun provideScarlet(application: AudioStreamerApplication, client: OkHttpClient, moshi: Moshi): Scarlet {
        val backoffStrategy = LinearBackoffStrategy(1000)
       // val BACKOFF_STRATEGY = ExponentialWithJitterBackoffStrategy(RETRY_BASE_DURATION, RETRY_MAX_DURATION)
        return Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(BASE_URL))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory(moshi))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory())
//            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
            .backoffStrategy(backoffStrategy)
            .build()
    }



    @Singleton
    @Provides
    fun provideAudioStreamerService(scarlet: Scarlet): AudioStreamerService {
        return scarlet.create()
    }
}
