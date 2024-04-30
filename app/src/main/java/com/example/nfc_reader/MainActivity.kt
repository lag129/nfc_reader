package com.example.nfc_reader

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val felica = FelicaReader(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        felica.setListener(felicaListener)
    }

    override fun onResume() {
        super.onResume()
        felica.start()
    }

    override fun onPause() {
        super.onPause()
        felica.stop()
    }

    private val felicaListener = object : FelicaReaderInterface {
        override fun onReadTag(tag : Tag) {
            val tvMain = findViewById<TextView>(R.id.textView)
            val idm : ByteArray = tag.id
            tag.techList
            tvMain.text = byteToHex(idm)
            Log.d("Sample", byteToHex(idm))
        }

        override fun onConnect() {
            Log.d("Sample", "onConnected")
        }
    }

    private fun byteToHex(b : ByteArray): String {
        var s = ""
        for (element in b) {
            s += "[%02X]".format(element)
        }
        return s
    }
}

interface FelicaReaderInterface : FelicaReader.Listener {
    fun onReadTag(tag : Tag)
    fun onConnect()
}

@Suppress("DEPRECATION")
class FelicaReader(private val context: Context, private val activity: Activity): android.os.Handler() {
    private var nfcManager: NfcManager? = null
    private var nfcAdapter: NfcAdapter? = null
    private var callback: CustomReaderCallback? = null
    private var listener: FelicaReaderInterface? = null
    interface Listener

    fun start() {
        callback = CustomReaderCallback()
        callback?.setHandler(this)

        nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager?.defaultAdapter
        nfcAdapter?.enableReaderMode(
            activity,
            callback,
            NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    fun stop() {
        nfcAdapter?.disableReaderMode(activity)
        callback = null
    }

    override fun handleMessage(msg: Message) {
        if (msg.arg1 == 1) {
            listener?.onReadTag(msg.obj as Tag)
        }
        if (msg.arg1 == 2) {
            listener?.onConnect()
        }
    }

    fun setListener(listener: Listener?) {
        if (listener is FelicaReaderInterface) {
            this.listener = listener
        }
    }

    private class CustomReaderCallback : NfcAdapter.ReaderCallback {
        private var handler: android.os.Handler? = null

        override fun onTagDiscovered(tag: Tag) {
            Log.d("Sample", tag.id.toString())

            val msg = Message.obtain()
            msg.arg1 = 1
            msg.obj = tag
            if (handler != null) {
                handler?.sendMessage(msg)
            }
            val nfc: NfcF = NfcF.get(tag) ?: return
            try {
                nfc.connect()
                nfc.close()
                msg.arg1 = 2
                msg.obj = tag
                if (handler != null) {
                    handler?.sendMessage(msg)
                }
            } catch (e: Exception) {
                nfc.close()
            }
        }

        fun setHandler(handler: android.os.Handler) {
            this.handler = handler
        }
    }

 }