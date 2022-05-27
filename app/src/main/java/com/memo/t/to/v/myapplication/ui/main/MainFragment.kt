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
        val granted =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_RECORD_AUDIO
            )
        } else {
            initRecognizer()

            // setOnClickListener でクリック動作を登録し、クリックで音声入力が開始するようにする
            binding.recognizeStartButton.setOnClickListener {
                startListening()
            }

            // setOnclickListner でクリック動作を登録し、クリックで音声入力が停止するようにする
            binding.recognizeStopButton.setOnClickListener { speechRecognizer?.stopListening() }

        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).also {
            it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
        println("デバッグ startListening!")
    }

    private fun initRecognizer() {
        speechRecognizer?.let {
            it.stopListening()
            it.cancel()
            it.destroy()
        }
        speechRecognizer = null

        // Activity での生成になるので、ApplicationContextを渡してやる
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(createRecognitionListenerStringStream(
            {
                println("デバッグ onResults $it")
                restartListening()
            },
            {
                println("デバッグ $it")
                val currentText = binding.recognizeTextView.text.toString()
                println("デバッグ 次のテキスト　${currentText + it}")
                binding.recognizeTextView.text = currentText + it
                restartListening()
            }
        ))
    }

    private fun restartListening() {
        initRecognizer()
        startListening()
    }

    // ライフサイクルにあわせて SpeechRecognizer を破棄する
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    /** 公開関数で受け取った TextView の更新処理を各関数で呼び出す*/
    private fun createRecognitionListenerStringStream(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit
    ): RecognitionListener {
        return object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) {
                /** 今回は特に利用しない */
            }

            override fun onReadyForSpeech(params: Bundle) {
            }

            override fun onBufferReceived(buffer: ByteArray) {
            }

            override fun onPartialResults(partialResults: Bundle) {
                val recData =
                    partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (recData != null && recData.isNotEmpty()) {
                    val text = recData.joinToString("")
                    if (text.isNotBlank()) {
                        onPartialResult(text)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onEndOfSpeech() {
                println("デバッグ end of speech")
            }

            override fun onError(error: Int) {
            }

            override fun onResults(results: Bundle) {
                val stringArray = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                onResult(stringArray.toString())
            }
        }
    }
}