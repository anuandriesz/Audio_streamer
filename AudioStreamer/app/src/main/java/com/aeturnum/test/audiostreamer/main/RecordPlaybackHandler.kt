package com.aeturnum.test.audiostreamer.main
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalStateException
import kotlin.concurrent.thread
import android.media.AudioAttributes

import android.media.MediaPlayer
import java.lang.IllegalArgumentException

/*
NOTE 1
 For applications where performance isn’t a priority, or audio makes up a small component of the functionality,
 MediaRecorder and MediaPlayer might be an ideal combination to capture and play back audio without writing much complex code.

 Note 2

  to perform some audio processing or otherwise need real-time audio, consider using AudioRecord and AudioTrack. The process of
  reading and writing data is more involved than using MediaRecorder, and any compression or
   transcoding can’t be done using the AudioRecord APIs.

Note 3
For audio playback option ExoPlayer -->useful for streaming media over the internet(Maintained by Google but not a part of SDK)
ExoPlayer doesn’t support PCM-encoded files (an easy fix is to add a WAV header to the raw file).

 */
class RecordPlaybackHandler(val context: Context) {
    var TAG = "RecordPlaybackHandler Class "
    var mediaRecorder: MediaRecorder? = null
    var mediaPlayer: MediaPlayer? = null
    var audioRecord: AudioRecord? = null
    var audioTrack: AudioTrack? = null

    private var recordingThread: Thread? = null
    private var playingThread: Thread? = null

    var isRecordingMedia = false
    var isPlayingMedia = false
    var isRecordingAudio = false
    var isPlayingAudio = false


    var fileNameMedia: String? =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +  "/$filename3gp}"
    var fileNameAudio: String? =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +  "/$filenamePcm}"
    //// Record ------------------------------------------------------------------------------
    fun startRecordingMedia(){
        if (!isRecordingMedia) {
            if (mediaRecorder == null) { // safety check, don't start a new recording if one is already going
                mediaRecorder = MediaRecorder()
                mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder!!.setOutputFile(fileNameMedia)
                mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                try {
                    mediaRecorder!!.prepare()
                } catch (e: IOException) {
                    // handle error
                    Log.e(TAG, "could not prepare MediaRecorder " + e.toString())
                    return
                } catch (e: IllegalStateException) {
                    // handle error
                    Log.e(TAG, "could not prepare MediaRecorder $e")
                    return
                }
                mediaRecorder!!.start()
                Log.d(TAG, "recording started with MediaRecorder")
                isRecordingAudio = true;

                recordingThread = thread(true) {
                    writeAudioDataToFile(filename3gp!!)
                }
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

    fun startRecordingAudio(){
        if (audioRecord == null) { // safety check
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                WavRecorder.RECORDER_SAMPLE_RATE, WavRecorder.RECORDER_CHANNELS,
                WavRecorder.RECORDER_AUDIO_ENCODING, BUFFER_SIZE_RECORDING
            )

            if (audioRecord!!.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing AudioRecord");
                Toast.makeText(context, "Couldn't initialize AudioRecord, check configuration", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord!!.startRecording();
            Log.d(TAG, "recording started with AudioRecord");

            isRecordingAudio = true;

            recordingThread = thread(true) {
                 writeAudioDataToFile(fileNameAudio!!)
            }
        }
    }
    fun stopRecordingAudio(){
        if (audioRecord != null) {
            isRecordingAudio = false; // triggers recordingThread to exit while loop
        }
    }
    //Write to file
    private fun writeAudioDataToFile(path:String) { // called inside Runnable of recordingThread
        val data =
            ByteArray(BUFFER_SIZE_RECORDING / 2) // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(path)
        } catch (e: FileNotFoundException) {
            // handle error
            Toast.makeText(context, "Couldn't find file to write to", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "file not found for file name " + fileNameAudio + ", " + e.toString())
            return
        }
        while (isRecordingAudio) {
            val read = audioRecord!!.read(data, 0, data.size)
            try {
                outputStream.write(data, 0, read)
            } catch (e: IOException) {
                Toast.makeText(context, "Couldn't write to file while recording", Toast.LENGTH_SHORT)
                    .show()
                Log.d(TAG, "IOException while recording with AudioRecord, $e")
                e.printStackTrace()
            }
        }
        try { // clean up file writing operations
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "exception while closing output stream $e")
            e.printStackTrace()
        }
        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null
        recordingThread = null
    }


    //// Play --------------------------------------------------------------------------------------
    fun playAudio(){
        if (audioTrack == null) {
            audioTrack = AudioTrack(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build(),
                AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                .setSampleRate(RECORDER_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
                BUFFER_SIZE_PLAYING,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
            );

            if (audioTrack!!.getState() != AudioTrack.STATE_INITIALIZED) {
                Toast.makeText(context, "Couldn't initialize AudioTrack, check configuration", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "error initializing AudioTrack");
                return;
            }

            audioTrack!!.play();
            Log.d(TAG, "playback started with AudioTrack");

            isPlayingAudio = true;

            playingThread = thread(true) {
                readAudioDataFromFile(fileNameAudio)
            }
        }
    }
    fun stopAudio(){
        isPlayingAudio = false
    }

    fun playMedia(){
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnCompletionListener { mp ->

                // release resources when end of file is reached
                mp.reset()
                mp.release()
                mediaPlayer = null
                isPlayingMedia = false
            }
            try {
                mediaPlayer!!.setDataSource(fileNameMedia)
                mediaPlayer!!.setAudioAttributes(
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                ) // optional step
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()

                isPlayingAudio = true;

                playingThread = thread(true) {
                    readAudioDataFromFile(fileNameMedia)
                }
                Log.d(TAG, "playback started with MediaPlayer")
            } catch (e: IOException) {
                Toast.makeText(
                    context,
                    "Couldn't prepare MediaPlayer, IOException",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "error reading from file while preparing MediaPlayer$e")
            } catch (e: IllegalArgumentException) {
                Toast.makeText(
                    context,
                    "Couldn't prepare MediaPlayer, IllegalArgumentException",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "illegal argument given $e")
            }
        }
    }
    fun stopMedia(){
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    //Read Audo from a file
    private  fun readAudioDataFromFile(path:String?) { // called inside Runnable of playingThread
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(path)
        } catch (e: IOException) {
            Toast.makeText(context, "Couldn't open file input stream, IOException", Toast.LENGTH_SHORT)
                .show()
            Log.e(TAG, "could not create input stream before using AudioTrack $e")
            e.printStackTrace()
            return
        }
        val data = ByteArray(BUFFER_SIZE_PLAYING / 2)
        var i = 0
        while (isPlayingAudio && i != -1) { // continue until run out of data or user stops playback
            try {
                i = fileInputStream.read(data)
                audioTrack!!.write(data, 0, i)
            } catch (e: IOException) {
                Toast.makeText(
                    context,
                    "Couldn't read from file while playing audio, IOException",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Could not read data $e")
                e.printStackTrace()
                return
            }
        }
        try { // finish file operations
            fileInputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close file input stream $e")
            e.printStackTrace()
            return
        }

        // clean up resources
        isPlayingAudio = false
        audioTrack!!.stop()
        audioTrack!!.release()
        audioTrack = null
        playingThread = null
    }

    companion object {
        // for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section
        const val RECORDER_SAMPLE_RATE = 44100
        const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
        val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLE_RATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING
        )
        val BUFFER_SIZE_PLAYING = AudioTrack.getMinBufferSize(
            RECORDER_SAMPLE_RATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING
        )
        val filenamePcm = "mclef_recording_${System.currentTimeMillis()}.pcm"
        val filename3gp = "mclef_recording_${System.currentTimeMillis()}.3gp"

    }
}