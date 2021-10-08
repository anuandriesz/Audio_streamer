package com.aeturnum.test.audiostreamer.main
import android.content.Context
import android.media.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.lang.IllegalStateException
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.aeturnum.test.audiostreamer.utils.Helper
import java.io.*
import android.media.MediaRecorder
import android.media.AudioManager
import android.net.Uri
import android.os.Build

/*
NOTE 1
 For applications where performance isn’t a priority, or audio makes up a small component of
 the functionality,
 MediaRecorder and MediaPlayer might be an ideal combination to capture and play back audio without
  writing much complex code.

 Note 2

  to perform some audio processing or otherwise need real-time audio, consider using AudioRecord and
   AudioTrack. The process of
  reading and writing data is more involved than using MediaRecorder, and any compression or
   transcoding can’t be done using the AudioRecord APIs.

Note 3
For audio playback option ExoPlayer -->useful for streaming media over the internet(Maintained by
 Google but not a part of SDK)
ExoPlayer doesn’t support PCM-encoded files (an easy fix is to add a WAV header to the raw file).

 */
class Mp3RecordPlaybackHandler(val context: Context) {
    private var TAG = "RecordPlaybackHandler Class "
    var mediaRecorder: MediaRecorder? = null
    var mediaPlayer: MediaPlayer? = null
    var audioTrack: AudioTrack? = null

    private var recordingThread: Thread? = null
    private var playingThread: Thread? = null

    var isRecordingMedia = false
    var isPlayingMedia = false


    var fileNameMediaMp3: String? =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +  "/$filenamemp3"
    //---------------------------------------------------Recording Audio ---------------------------
    fun startRecordingMedia(){
        Helper.setLastRecFilePath(fileNameMediaMp3!!)
        if (!isRecordingMedia) {
            // record with MediaPlayer
            if (mediaRecorder == null) {
                // safety check, don't start a new recording if one is already going
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mediaRecorder =  MediaRecorder(context)
                }else {
                    mediaRecorder =  MediaRecorder()
                }
                mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder!!.setAudioSamplingRate(RECORDER_SAMPLE_RATE)
                mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder!!.setOutputFile(fileNameMediaMp3)
                mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                try {
                    mediaRecorder!!.prepare()
                } catch (e: IOException) {
                    // handle error
                    Toast.makeText(
                        context,
                        "IOException while trying to prepare MediaRecorder",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "could not prepare MediaRecorder $e")
                    return
                } catch (e: IllegalStateException) {
                    // handle error
                    Toast.makeText(
                        context,
                        "IllegalStateException while trying to prepare MediaRecorder",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "could not prepare MediaRecorder $e")
                    return
                }
                mediaRecorder!!.start()
                Log.d(TAG, "recording started with MediaRecorder")
            }
        } else {
            stopRecordingMedia()
        }
        isRecordingMedia = !isRecordingMedia
    }
    fun stopRecordingMedia(){
        if (mediaRecorder != null) {
            // stop recording and free up resources
            mediaRecorder!!.stop();
            mediaRecorder!!.reset();
            mediaRecorder!!.release();
            mediaRecorder = null;
        }
    }

    //---------------------------------------------------Playing Audio------------------------------
    fun playMedia(){
        val uri =  Uri.parse(Helper.getLastRecFilePath())
         mediaPlayer = MediaPlayer.create(context,uri).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
           // setDataSource(context, uri)
           // prepare()
            start()
        }
    }
    fun stopMedia(){
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }
    companion object {
        //for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section
        const val RECORDER_SAMPLE_RATE = 44100
        const val NUMBER_CHANNELS: Short = 1
        val filenamePcm = "mclef_recording_${System.currentTimeMillis()}.pcm"
        val filenamemp3 = "mclef_recording_${System.currentTimeMillis()}.wav"
        val fileNameWav = "mclef_recording_${System.currentTimeMillis()}.wav"

    }
}