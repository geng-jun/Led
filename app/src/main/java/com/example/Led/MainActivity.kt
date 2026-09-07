package com.example.Led

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var etIpAddress: TextInputEditText
    private lateinit var etPortNumber: TextInputEditText
    private lateinit var btnLedOn: MaterialButton
    private lateinit var btnLedOff: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var ivInfoIcon: ImageView

    private var isSending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        etIpAddress = findViewById(R.id.etIpAddress)
        etPortNumber = findViewById(R.id.etPortNumber)
        btnLedOn = findViewById(R.id.btnLedOn)
        btnLedOff = findViewById(R.id.btnLedOff)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        ivInfoIcon = findViewById(R.id.ivInfoIcon)

        // 傳入布林值：true 代表開燈，false 代表關燈
        btnLedOn.setOnClickListener { sendCommand(true) }
        btnLedOff.setOnClickListener { sendCommand(false) }
    }

    private fun sendCommand(turnOn: Boolean) {
        val ipAddress = etIpAddress.text?.toString()?.trim().orEmpty()
        val portNumber = etPortNumber.text?.toString()?.trim().orEmpty()

        if (ipAddress.isBlank() || portNumber.isBlank()) {
            tvStatus.text = "請填寫IP與Port"
            return
        }

        val port = portNumber.toIntOrNull() ?: 5000
        val actionText = if (turnOn) "開燈" else "關燈"
        setSendingState(true, "正在連線至 $ipAddress:$port ...")

        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ipAddress, port), 3000)

                outputStream = socket.getOutputStream()

                // 發送單個位元組：true -> 0x01, false -> 0x00
                val payload = byteArrayOf(if (turnOn) 1 else 0)
                outputStream.write(payload)
                outputStream.flush()

                withContext(Dispatchers.Main) {
                    tvStatus.text = "指令 [$actionText] 已成功送出"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "錯誤: ${e.localizedMessage ?: "未知錯誤"}"
                }
            } finally {
                try {
                    outputStream?.close()
                    socket?.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "${e.localizedMessage ?: "未知錯誤"}"
                    }
                }

                withContext(Dispatchers.Main) {
                    setSendingState(false)
                }
            }
        }
    }

    private fun setSendingState(sending: Boolean, statusMessage: String? = null) {
        isSending = sending
        btnLedOn.isEnabled = !sending
        btnLedOff.isEnabled = !sending

        if (sending) {
            progressBar.visibility = View.VISIBLE
            ivInfoIcon.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            ivInfoIcon.visibility = View.VISIBLE
        }
        if (statusMessage != null) {
            tvStatus.text = statusMessage
        }
    }
}