package com.aeturnum.test.audiostreamer.main
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aeturnum.test.audiostreamer.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.*
@AndroidEntryPoint
class MainFragment: Fragment() {
    private lateinit var mediaPlayer: MediaPlayer
    private val selectAudio = 2
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: MainFragmentBinding
    private val PERMISSION_REQUEST_CODE = 5
    private var audioRecordingStart = false
    private var isMp3RecorderStarted = false
    private var isAudioPlaying = false
    companion object {
        fun newInstance() = MainFragment()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireContext() as Activity,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
        binding.btStartStreamingAudio.setOnClickListener {
            openGalleryAudio()
        }

        //.wave recording event handler
        binding.btAudioRecording.setOnClickListener {
            if (!audioRecordingStart) {
                binding.btStartStreamingAudio.visibility = View.GONE
                binding.btStopAudioStreaming.visibility = View.GONE
                audioRecordingStart = true
                binding.btAudioRecording.text = "Stop Recoding "
                RecordingHandler(requireContext()).startRecordingAudio()
            } else {
                audioRecordingStart = false
                binding.btAudioRecording.text = " Start Recording "
                binding.txtAudioInfo.text = "Audio recording stopped .."
                RecordingHandler(requireContext()).stopRecordingAudio()
                binding.btStartStreamingAudio.visibility = View.VISIBLE
            }
        }

        //Audio play /pause function .wave
        binding.btStartStreamingAudio.setOnClickListener {
            binding.txtAudioInfo.text = "Audio streaming  started .."
            RecordingHandler(requireContext()).streaming()
        }
        binding.btStopAudioStreaming.setOnClickListener {
            RecordingHandler(requireContext()).stopAudio()
        }
        viewModel.audioDataReceived.observe(viewLifecycleOwner, {
            binding.btStopAudioStreaming.visibility = View.VISIBLE
            binding.txtAudioInfo.text = "Audio data received.."
            if (it != null) {
                RecordingHandler(requireContext()).playAudio(it)
            } else {
                binding.txtAudioInfo.text =
                    "Wrong Audio data format. Please check with the server.."
            }
        })
        viewModel.textDataReceived.observe(viewLifecycleOwner, {
            binding.txtAudioInfo.text = "$it"
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.txtAudioInfo.text = "Audio recording .."
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun sendAudio(data: ByteArray) {
        doAsync {
            // do your background thread task
            viewModel.sendAudioData(data)
            uiThread {
                // use result here if you want to update ui
                binding.txtAudioInfo.text = "Audio Upload in progress.."
            }
        }
    }

    //open Mobile gallery
    private fun openGalleryAudio() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Audio "), selectAudio)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == selectAudio) {
                println("SELECT_AUDIO")
                val selectedImageUri: Uri? = data!!.data
                val file = File(
                    createCopyAndReturnRealPath(
                        requireContext(),
                        selectedImageUri!!
                    ).toString()
                )
                //binding.txtAudioInfo.text = "Audio Selected and ready to stream .."
                val binaryAudioData = audioToBinaryConverter(file)
                //PUBLISH Data
                binding.txtAudioInfo.text = "Audio data uploading .."
                sendAudio(binaryAudioData)
            }
        }
    }

    //get real file path of selected audio
    private fun createCopyAndReturnRealPath(@NonNull context: Context, @NonNull uri: Uri?): String? {
        val contentResolver: ContentResolver = context.contentResolver ?: return null

        // Create file path inside app's data dir
        val filePath: String =
            context.applicationInfo.dataDir + File.separator.toString() + "temp_file"
        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(uri!!) ?: return null
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (ignore: IOException) {
            return null
        }
        return file.absolutePath
    }

    private fun audioToBinaryConverter(audioFilePath: File): ByteArray {
        val bos = ByteArrayOutputStream()
        try {
            val fis = DataInputStream(FileInputStream(audioFilePath))
            val b = ByteArray(1024)
            var readNum: Int
            while (fis.read(b).also { readNum = it } != -1) {
                bos.write(b, 0, readNum)
            }

        } catch (e: java.lang.Exception) {
            Timber.d(e.toString())
        }
        return bos.toByteArray()
    }
}
