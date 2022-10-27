package pe.aioo.openmoa

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kimkevin.hangulparser.HangulParser
import com.github.kimkevin.hangulparser.HangulParserException

class OpenMoaIME : InputMethodService() {

    private lateinit var broadcastReceiver: BroadcastReceiver
    private val jamoList: ArrayList<String> = arrayListOf()

    override fun onCreate() {
        super.onCreate()
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val key = intent.getStringExtra(EXTRA_NAME) ?: return
                // Process for special key
                if (key.length > 1) {
                    when (key) {
                        SpecialKey.BACKSPACE.value -> {
                            if (jamoList.size > 0) {
                                jamoList.removeLast()
                                val assembled = try {
                                    HangulParser.assemble(jamoList)
                                } catch (_: HangulParserException) {
                                    jamoList.joinToString("")
                                }
                                currentInputConnection.setComposingText(assembled, 1)
                            } else {
                                currentInputConnection.deleteSurroundingText(1, 0)
                            }
                        }
                        SpecialKey.ENTER.value -> currentInputConnection.performEditorAction(
                            EditorInfo.IME_ACTION_GO
                        )
                    }
                    return
                }
                // Process for non-Hangul key
                if (!key.matches(Regex("^[ㄱ-ㅎㅏ-ㅣ]$"))) {
                    if (jamoList.size > 0) {
                        val assembled = try {
                            HangulParser.assemble(jamoList)
                        } catch (_: HangulParserException) {
                            jamoList.joinToString("")
                        }
                        currentInputConnection.commitText(assembled, 1)
                        jamoList.clear()
                    }
                    currentInputConnection.commitText(key, 1)
                    return
                }
                // Process for Hangul key
                jamoList.add(key)
                if (jamoList.size == 1) {
                    currentInputConnection.setComposingText(key, 1)
                    return
                }
                try {
                    val assembled = HangulParser.assemble(jamoList)
                    if (assembled.length > 1) {
                        val composed = assembled.substring(0, 1)
                        currentInputConnection.commitText(composed, 1)
                        currentInputConnection.setComposingText(
                            assembled.substring(1, 2), 1
                        )
                        for (i in 0 until HangulParser.disassemble(composed).size) {
                            jamoList.removeFirst()
                        }
                        return
                    }
                    currentInputConnection.setComposingText(assembled, 1)
                } catch (e: HangulParserException) {
                    val prevJamoList = jamoList.subList(0, jamoList.size - 1)
                    val assembled = try {
                        HangulParser.assemble(prevJamoList)
                    } catch (_: HangulParserException) {
                        prevJamoList.joinToString("")
                    }
                    for (i in 0 until prevJamoList.size) {
                        jamoList.removeFirst()
                    }
                    currentInputConnection.commitText(assembled, 1)
                    currentInputConnection.setComposingText(jamoList.first(), 1)
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver, IntentFilter(INTENT_ACTION)
        )
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        window.window?.apply {
            navigationBarColor =
                ContextCompat.getColor(this@OpenMoaIME, R.color.keyboard_background)
            when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    insetsController?.apply {
                        setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)
                    }
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    insetsController?.apply {
                        setSystemBarsAppearance(
                            APPEARANCE_LIGHT_NAVIGATION_BARS, APPEARANCE_LIGHT_NAVIGATION_BARS
                        )
                    }
                }
            }
        }
        return layoutInflater.inflate(R.layout.open_moa_ime, null)
    }

    override fun onDestroy() {
        if (this::broadcastReceiver.isInitialized) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        }
        super.onDestroy()
    }

    companion object {
        const val INTENT_ACTION = "keyInput"
        const val EXTRA_NAME = "key"
    }

}