package com.aeturnum.test.audiostreamer.main
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioManager.STREAM_MUSIC
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.aeturnum.test.audiostreamer.utils.Helper
import java.io.*
import kotlin.concurrent.thread
/*
NOTE 1
Problems
---> Recording not clear enough
---> Playback exception
 */
class WaveRecordingHandler(val context: Context): Activity() {
    private var TAG = "WaveRecordingHandler Class "
    var audioRecord: AudioRecord? = null
    var audioTrack: AudioTrack? = null

    private var recordingThread: Thread? = null
    private var playingThread: Thread? = null

    var isRecordingAudio = false
    var isPlayingAudio = false

    // var fileNameAudio: String? =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +  "/$filenamePcm"
     //var fileNameWave: String? =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +  "/$fileNameWav"
     var fileNameWave =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/$filenamePcm" + ".pcm"
    //---------------------------------------------------Recording Audio --------------------------------------------------------------------------
    fun startRecordingAudio(){
        val fileAudio = File(fileNameWave)
        if (!fileAudio.exists()) { // create empty files if needed
            try {
                fileAudio.createNewFile()
            } catch (e: IOException) {
                Log.d(TAG, "could not create file $e")
                e.printStackTrace()
            }
        }
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
                RECORDER_SAMPLE_RATE,RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BUFFER_SIZE_RECORDING
            )

            if (audioRecord!!.state != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing AudioRecord");
                Toast.makeText(context, "Couldn't initialize AudioRecord, check configuration", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord!!.startRecording();
            Log.d(TAG, "recording started with AudioRecord");

            isRecordingAudio = true;
            fileNameWave?.let { Helper.setLastRecFilePath(it) }
            recordingThread = thread(true) {
                //writeAudioDataToFile(fileNameWave!!)
                writeAudioDataToFile()
            }
        }
    }
    fun stopRecordingAudio(){
        if (audioRecord != null) {
            isRecordingAudio = false; // triggers recordingThread to exit while loop
        }
    }
    //pcm
    private fun writeAudioDataToFile() { // called inside Runnable of recordingThread
        val data =
            ByteArray(BUFFER_SIZE_RECORDING / 2)
        // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(fileNameWave)
        } catch (e: FileNotFoundException) {
            // handle error
            Toast.makeText(this, "Couldn't find file to write to", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "file not found for file name $fileNameWave, $e")
            return
        }
        while (isRecordingAudio) {
            val read = audioRecord!!.read(data, 0, data.size)
            try {
                outputStream!!.write(data, 0, read)
            } catch (e: IOException) {
                Toast.makeText(this, "Couldn't write to file while recording", Toast.LENGTH_SHORT)
                    .show()
                Log.d(TAG, "IOException while recording with AudioRecord, $e")
                e.printStackTrace()
            }
        }
        try { // clean up file writing operations
            outputStream!!.flush()
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
    //Write to file .wav
    private fun writeAudioDataToFile(path:String) {
        val dataWav = arrayListOf<Byte>()
        val data = ByteArray(BUFFER_SIZE_RECORDING / 2)
        // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size

        var wavOut: FileOutputStream? = null
        try {
            wavOut = FileOutputStream(path)
            for (byte in wavFileHeader()) {
                dataWav.add(byte)
            }
        } catch (e: FileNotFoundException) {
            // handle error
            Toast.makeText(context, "Couldn't find file to write to", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "file not found for file name " + fileNameWave + ", " + e.toString())
            return
        }

        while (isRecordingAudio) {
            val read = audioRecord!!.read(data, 0, data.size)

            try {
                for (byte in data)
                    dataWav.add(byte)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            updateHeaderInformation(dataWav)

            try {
                wavOut.write(dataWav.toByteArray(), 0, read)
            } catch (e: IOException) {
                Toast.makeText(context, "Couldn't write to file while recording", Toast.LENGTH_SHORT)
                    .show()
                Log.d(TAG, "IOException while recording with AudioRecord, $e")
                e.printStackTrace()
            }
        }

        try { // clean up file writing operations
            wavOut.flush()
            wavOut.close()
        } catch (e: IOException) {
            Log.e(TAG, "exception while closing output stream $e")
            e.printStackTrace()
        }
        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null
        recordingThread = null
    }
    /**
     * Constructs header for wav file format
     */
    private fun wavFileHeader(): ByteArray {
        val headerSize = 44
        val header = ByteArray(headerSize)

        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()

        header[4] = (0 and 0xff).toByte() // Size of the overall file, 0 because unknown
        header[5] = (0 shr 8 and 0xff).toByte()
        header[6] = (0 shr 16 and 0xff).toByte()
        header[7] = (0 shr 24 and 0xff).toByte()

        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()

        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()

        header[16] = 16 // Length of format data
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1 // Type of format (1 is PCM)
        header[21] = 0

        header[22] = NUMBER_CHANNELS.toByte()
        header[23] = 0

        header[24] = (RECORDER_SAMPLE_RATE and 0xff).toByte() // Sampling rate
        header[25] = (RECORDER_SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (RECORDER_SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (RECORDER_SAMPLE_RATE shr 24 and 0xff).toByte()

        header[28] = (BYTE_RATE and 0xff).toByte() // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
        header[29] = (BYTE_RATE shr 8 and 0xff).toByte()
        header[30] = (BYTE_RATE shr 16 and 0xff).toByte()
        header[31] = (BYTE_RATE shr 24 and 0xff).toByte()

        header[32] = (NUMBER_CHANNELS * BITS_PER_SAMPLE / 8).toByte() //  16 Bits stereo
        header[33] = 0

        header[34] = BITS_PER_SAMPLE.toByte() // Bits per sample
        header[35] = 0

        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()

        header[40] = (0 and 0xff).toByte() // Size of the data section.
        header[41] = (0 shr 8 and 0xff).toByte()
        header[42] = (0 shr 16 and 0xff).toByte()
        header[43] = (0 shr 24 and 0xff).toByte()

        return header
    }
    private fun updateHeaderInformation(data: ArrayList<Byte>) {
        val fileSize = data.size
        val contentSize = fileSize - 44

        data[4] = (fileSize and 0xff).toByte() // Size of the overall file
        data[5] = (fileSize shr 8 and 0xff).toByte()
        data[6] = (fileSize shr 16 and 0xff).toByte()
        data[7] = (fileSize shr 24 and 0xff).toByte()

        data[40] = (contentSize and 0xff).toByte() // Size of the data section.
        data[41] = (contentSize shr 8 and 0xff).toByte()
        data[42] = (contentSize shr 16 and 0xff).toByte()
        data[43] = (contentSize shr 24 and 0xff).toByte()
    }
    //---------------------------------------------------Playing Audio----------------------------------------------------------------------------------
    fun playAudio(){
        if (audioTrack == null) {
            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC )
                    .setUsage(AudioAttributes.USAGE_MEDIA).build(),
                AudioFormat.Builder()
                    .setEncoding(RECORDER_AUDIO_ENCODING)
                    .setSampleRate(RECORDER_SAMPLE_RATE)
                    .setChannelMask(PLAYBACK_CHANNEL)
                    .build(),
                AudioTrack.getMinBufferSize(RECORDER_SAMPLE_RATE,PLAYBACK_CHANNEL,RECORDER_AUDIO_ENCODING),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            );

            if (audioTrack!!.state != AudioTrack.STATE_INITIALIZED) {
                Toast.makeText(context, "Couldn't initialize AudioTrack, check configuration", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "error initializing AudioTrack")
                return;
            }

            audioTrack!!.play();
            Log.d(TAG, "playback started with AudioTrack")

            isPlayingAudio = true;

            playingThread = thread(true) {
                readAudioDataFromFile(fileNameWave)
            }
        }
    }
    fun stopAudio(){
        isPlayingAudio = false
    }

    //Read Audio from a file
    private  fun readAudioDataFromFile(path:String?) {
        var fileInputStream: FileInputStream? = null
        fileInputStream = try {
            FileInputStream(path)
        } catch (e: IOException) {
            //Toast.makeText(this, "Couldn't open file input stream, IOException", Toast.LENGTH_SHORT)
               // .show()
            Log.d(TAG, "could not create input stream before using AudioTrack $e")
            e.printStackTrace()
            return
        }

        val data = ByteArray(BUFFER_SIZE_PLAYING / 2)
        var i = 0

        while (isPlayingAudio && i != -1) { // continue until run out of data or user stops playback
            try {
                i = fileInputStream!!.read(data)
                audioTrack!!.write(data, 0, i)
            } catch (e: IOException) {
                Toast.makeText(
                    this,
                    "Couldn't read from file while playing audio, IOException",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Could not read data $e")
                e.printStackTrace()
                return
            }
        }
        try { // finish file operations
            fileInputStream!!.close()
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
        const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_STEREO
        const val PLAYBACK_CHANNEL: Int = android.media.AudioFormat.CHANNEL_OUT_STEREO
        const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
        val BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLE_RATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING
        )
        val BUFFER_SIZE_PLAYING = AudioTrack.getMinBufferSize(
            RECORDER_SAMPLE_RATE,PLAYBACK_CHANNEL,RECORDER_AUDIO_ENCODING
        )
        const val BITS_PER_SAMPLE: Short = 16
        const val NUMBER_CHANNELS: Short = 1
        const val BYTE_RATE = RECORDER_SAMPLE_RATE * NUMBER_CHANNELS * 16 / 8

        val filenamePcm = "mclef_recording_${System.currentTimeMillis()}"
        val filenamemp3 = "mclef_recording_${System.currentTimeMillis()}.3gp"
        val fileNameWav = "mclef_recording_${System.currentTimeMillis()}"

    }
}