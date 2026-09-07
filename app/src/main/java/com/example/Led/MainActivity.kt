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

        btnLedOn.setOnClickListener { sendCommand("LED_ON") }
        btnLedOff.setOnClickListener { sendCommand("LED_OFF") }
    }

    private fun sendCommand(command: String) {
        val ipAddress = etIpAddress.text?.toString()?.trim().orEmpty()
        val portNumber = etPortNumber.text?.toString()?.trim().orEmpty()

        if (ipAddress.isBlank() || portNumber.isBlank()) {
            tvStatus.text = "請填寫IP與Port"
            return
        }

        val port = portNumber.toIntOrNull() ?: 5000
        setSendingState(true, "正在連線至 $ipAddress:$port ...")

        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            var outputStream: OutputStream? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ipAddress, port), 3000)

                outputStream = socket.getOutputStream()
                outputStream.write(command.toByteArray(Charsets.UTF_8))
                outputStream.flush()

                withContext(Dispatchers.Main) {
                    tvStatus.text = "指令 [$command] 已成功送出"
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