package com.aeturnum.test.audiostreamer.main
import android.content.ContentResolver
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.aeturnum.test.audiostreamer.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.*

@AndroidEntryPoint
class MainFragment: Fragment() {
    private lateinit var  mediaPlayer:MediaPlayer
    private val selectAudio = 2
    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: MainFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btStartStreamingAudio.setOnClickListener{
            openGalleryAudio()
        }
        viewModel.audioDataReceived.observe(viewLifecycleOwner, {
            binding.btStopAudio.visibility = View.VISIBLE
            binding.txtAudioInfo.text = "Audio data received.."
            if (it != null) {
                playMp3(it)
            }else {
                binding.txtAudioInfo.text = "Wrong Audio data format. Please check with the server.."
            }
        })
    }
    //open Mobile gallery
    private fun  openGalleryAudio(){
       val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select Audio "), selectAudio)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == selectAudio) {
                println("SELECT_AUDIO")
                val selectedImageUri: Uri? = data!!.data
                val file = File(createCopyAndReturnRealPath(requireContext(),selectedImageUri!!).toString())
                //binding.txtAudioInfo.text = "Audio Selected and ready to stream .."
                val binaryAudioData =  audioToBinaryConverter(file)
                //PUBLISH Data
                binding.txtAudioInfo.text = "Audio data uploading .."
                //fixme DATA PUBLISH.....................
                //getService().sendAudio(binaryAudioData)
            }
        }
    }
    //get real file path of selected audio
    private fun createCopyAndReturnRealPath(
        @NonNull context: Context, @NonNull uri: Uri?
    ): String? {
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
    private fun audioToBinaryConverter(audioFilePath:File): ByteArray{
        val bos = ByteArrayOutputStream()
        try {
            var fis =DataInputStream(FileInputStream(audioFilePath))
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
    //Convert and play received audio data
    private fun playMp3(mp3SoundByteArray: ByteArray) {
        try {
            mediaPlayer = MediaPlayer()
            binding.txtAudioInfo.text =  "Audio Data Processing... "
            // create temp file that will hold byte array
            val tempMp3 = File.createTempFile("AStreamer", "mp3", context?.cacheDir)
            tempMp3.deleteOnExit()
            val fos = FileOutputStream(tempMp3)
            fos.write(mp3SoundByteArray)
            fos.close()

            // resetting mediaplayer instance to evade problems
            mediaPlayer.reset()
            binding.txtAudioInfo.text =  "Media Player Starting... "

            // In case you run into issues with threading consider new instance like:
            // MediaPlayer mediaPlayer = new MediaPlayer();

            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            val fis = FileInputStream(tempMp3)
            mediaPlayer.setDataSource(fis.fd)
            mediaPlayer.prepare()
            binding.txtAudioInfo.text =  "Audio streaming... "
            mediaPlayer.start()

        } catch (ex: IOException) {
            val s = ex.toString()
            ex.printStackTrace()
        }
    }
}
