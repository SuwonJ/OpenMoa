package pe.aioo.openmoa.view.keytouchlistener

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.aioo.openmoa.OpenMoaIME
import pe.aioo.openmoa.R
import pe.aioo.openmoa.config.Config
import pe.aioo.openmoa.view.message.BaseKeyMessage
import pe.aioo.openmoa.view.message.SpecialKeyMessage
import pe.aioo.openmoa.view.message.StringKeyMessage

open class BaseKeyTouchListener(context: Context) : OnTouchListener, KoinComponent {

    protected val config: Config by inject()

    private val broadcastManager = LocalBroadcastManager.getInstance(context)
    private val backgrounds = listOf(
        ContextCompat.getDrawable(context, R.drawable.key_background_pressed),
        ContextCompat.getDrawable(context, R.drawable.key_background), ///////////키 배경
        ContextCompat.getDrawable(context, R.drawable.key_background_acc1),
    )

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                if( view.background==backgrounds[0] || view.background==backgrounds[1])     view.background = backgrounds[0]
                else if( view.background==backgrounds[0] || view.background==backgrounds[2])     view.background = backgrounds[0]
                if (config.hapticFeedback) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if( view.background==backgrounds[0] || view.background==backgrounds[1])     view.background = backgrounds[1]
                else if( view.background==backgrounds[0] || view.background==backgrounds[2])     view.background = backgrounds[2]
            }
            MotionEvent.ACTION_UP -> {
                if( view.background==backgrounds[0] || view.background==backgrounds[1])     view.background = backgrounds[1]
                else if( view.background==backgrounds[0] || view.background==backgrounds[2])     view.background = backgrounds[2]
                view.performClick()
            }
        }
        return true
    }

    protected fun sendKeyMessage(keyMessage: BaseKeyMessage) {
        broadcastManager.sendBroadcast(
            Intent(OpenMoaIME.INTENT_ACTION).apply {
                putExtra(OpenMoaIME.EXTRA_NAME, when (keyMessage) {
                    is StringKeyMessage -> keyMessage.key
                    is SpecialKeyMessage -> keyMessage.key
                    else -> ""
                })
            }
        )
    }

}