package com.memo.t.to.v.myapplication.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.memo.t.to.v.myapplication.R
import com.memo.t.to.v.myapplication.databinding.MainFragmentBinding
import timber.log.Timber

class MainFragment : Fragment() {

    companion object {
        private const val PERMISSIONS_RECORD_AUDIO = 1000
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding
    private val viewModel: MainViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.main_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.plant(Timber.DebugTree())
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_RECORD_AUDIO
            )
        } else {
            // Activity での生成になるので、ApplicationContextを渡してやる
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListenerStringStream {
                binding.recognizeTextView.text = it
            })

            // setOnClickListener でクリック動作を登録し、クリックで音声入力が開始するようにする
            binding.recognizeStartButton.setOnClickListener {
                speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
            }

            // setOnclickListner でクリック動作を登録し、クリックで音声入力が停止するようにする
            binding.recognizeStopButton.setOnClickListener { speechRecognizer?.stopListening() }

        }
    }

    // ライフサイクルにあわせて SpeechRecognizer を破棄する
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    /** 公開関数で受け取った TextView の更新処理を各関数で呼び出す*/
    private fun createRecognitionListenerStringStream(onResult : (String)-> Unit) : RecognitionListener {
        return object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) { /** 今回は特に利用しない */ }
            override fun onReadyForSpeech(params: Bundle) { onResult("onReadyForSpeech") }
            override fun onBufferReceived(buffer: ByteArray) { onResult("onBufferReceived") }
            override fun onPartialResults(partialResults: Bundle) { onResult("onPartialResults") }
            override fun onEvent(eventType: Int, params: Bundle) { onResult("onEvent") }
            override fun onBeginningOfSpeech() { onResult("onBeginningOfSpeech") }
            override fun onEndOfSpeech() { onResult("onEndOfSpeech") }
            override fun onError(error: Int) { onResult("onError") }
            override fun onResults(results: Bundle) {
                val stringArray = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                onResult("onResults " + stringArray.toString())
            }
        }
    }
}