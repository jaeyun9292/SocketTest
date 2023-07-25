package com.example.sockettestapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.sockettestapp.databinding.ActivityMainBinding
import com.uber.rxdogtag.RxDogTag
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import java.io.IOException

class MainActivity : AppCompatActivity(), View.OnClickListener, OnServerInteractionListener {
    private val TAG = "MainActivity"
    private var isBinding = false
    private var serviceClass: Class<*>? = null
    var mServiceManager: ServiceManager? = null
    private var isKill = false
    var isServerConnected = false
    private lateinit var ip: String
    private lateinit var port: String
    private lateinit var binding: ActivityMainBinding

    private var btn1Value = 0
    private var btn2Value = 0
    private var btn3Value = 0
    private var btn4Value = 0
    private var btn5Value = 0

    companion object {
        lateinit var prefs: PreferenceUtil
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RxDogTag.install()
        setRxJavaErrorHandler()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        serviceClass = ServiceManager::class.java
        prefs = PreferenceUtil(applicationContext)

        binding.ipEt.setText(prefs.getString("ip", "192.168.11.66"))
        binding.portEt.setText(prefs.getString("port", "8181"))

        binding.startBtn.setOnClickListener(this)
        binding.stopBtn.setOnClickListener(this)
        binding.sendBtn1.setOnClickListener(this)
        binding.sendBtn2.setOnClickListener(this)
        binding.sendBtn3.setOnClickListener(this)
        binding.sendBtn4.setOnClickListener(this)
        binding.sendBtn5.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.start_btn -> {
                if (!isBinding) {
                    ip = binding.ipEt.text.toString()
                    port = binding.portEt.text.toString()
                    prefs.setString("ip", ip)
                    prefs.setString("port", port)
                    setBind()
                    isBinding = true
                }
            }

            R.id.stop_btn -> {
                if (isBinding) {
                    setUnBind()
                    isBinding = false
                }
            }

            R.id.send_btn_1 -> {
                btn1Value++
                if (btn1Value > 1) btn1Value = 0
                if (btn1Value == 0) {
                    binding.sendBtn1.text = getString(R.string.send_A_on_text)
                    onTestInteraction("sn01_off_000")
                } else {
                    binding.sendBtn1.text = getString(R.string.send_A_off_text)
                    onTestInteraction("sn01_on_000")
                }
            }

            R.id.send_btn_2 -> {
                btn2Value++
                if (btn2Value > 1) btn2Value = 0
                if (btn2Value == 0) {
                    binding.sendBtn2.text = getString(R.string.send_B_on_text)
                    onTestInteraction("sn02_off_000")
                } else {
                    binding.sendBtn2.text = getString(R.string.send_B_off_text)
                    onTestInteraction("sn02_on_000")
                }
            }

            R.id.send_btn_3 -> {
                btn3Value++
                if (btn3Value > 1) btn3Value = 0
                if (btn3Value == 0) {
                    binding.sendBtn3.text = getString(R.string.send_C_on_text)
                    onTestInteraction("sn03_off_000")
                } else {
                    binding.sendBtn3.text = getString(R.string.send_C_off_text)
                    onTestInteraction("sn03_on_000")
                }
            }

            R.id.send_btn_4 -> {
                btn4Value++
                if (btn4Value > 2) btn4Value = 0
                when (btn4Value) {
                    0 -> {
                        binding.sendBtn4.text = getString(R.string.send_D_low_text)
                        onTestInteraction("sn04_on_130")
                    }
                    1 -> {
                        binding.sendBtn4.text = getString(R.string.send_D_medium_text)
                        onTestInteraction("sn04_on_70")
                    }
                    else -> {
                        binding.sendBtn4.text = getString(R.string.send_D_high_text)
                        onTestInteraction("sn04_on_90")
                    }
                }
            }

            R.id.send_btn_5 -> {
                btn5Value++
                if (btn5Value > 1) btn5Value = 0
                if (btn5Value == 0) {
                    binding.sendBtn5.text = getString(R.string.send_E_on_text)
                    onTestInteraction("sn05_off_000")
                } else {
                    binding.sendBtn5.text = getString(R.string.send_E_off_text)
                    onTestInteraction("sn05_on_000")
                }
            }
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.e("mServiceConnection", "onServiceConnected: ")
            mServiceManager = (service as ServiceManager.LocalBinder).getService()
            mServiceManager?.addListener(mServiceManagerListener)
            if (!isServerConnected) {
                mServiceManager?.onTcpConnect()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e("mServiceConnection", "onServiceDisConnected: ")
            mServiceManager?.removeListener()
            mServiceManager = null
        }
    }

    private val mServiceManagerListener: ServiceManager.Listener = object : ServiceManager.Listener {
        override fun onReceiveData(data: String?) {
            Log.e(TAG, "onReceiveData : $data")
            showDebugLog(data)
//            val maxLen = 2000 // 2000 bytes 마다 끊어서 출력
//            val len: Int = data!!.length
//            if (len > maxLen) {
//                var idx = 0
//                var nextIdx = 0
//                while (idx < len) {
//                    nextIdx += maxLen
//                    idx = nextIdx
//                }
//            }
//            val header = data.split("§")[0]
//            val body = data.split("§")[1]
//            val api = header.split("&")
//            val command = api[0].split("=")[1]
//            val action = api[1].split("=")[1]
//            val method = api[2].split("=")[1]
//            val result = body.split("&")
        }

        override fun onConnected(isConnected: Boolean) {
            Log.e(TAG, "onConnected: $isConnected")
            isServerConnected = isConnected
        }
    }

    fun showDebugLog(data: String?) {
        with(binding) {
            debugTv.append((data ?: "Null") + "\n")
            scrollview.post {
                scrollview.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun setRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            var error = e
            if (error is UndeliverableException) {
                error = e.cause
            }
            if (error is IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (error is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if (error is NullPointerException || error is IllegalArgumentException) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
            if (error is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler?.uncaughtException(Thread.currentThread(), error)
                return@setErrorHandler
            }
        }
    }

    override fun onServerInteraction(header: String, body: String) {
        mServiceManager?.sendMessage(header, body)
    }

    override fun onTestInteraction(testStr: String) {
        mServiceManager?.sendMessage(testStr)
    }

    private fun setBind() {
        Log.e(TAG, "setBind: ")
        bindService(
            Intent(this, serviceClass), mServiceConnection, BIND_AUTO_CREATE
        )
    }

    private fun setUnBind() {
        Log.e(TAG, "setUnBind: ")
        unbindService(
            mServiceConnection
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")
        if (mServiceManager != null) {
            unbindService(mServiceConnection)
            mServiceManager?.removeListener()
            mServiceManager = null
            isServerConnected = false
        }

        if (isKill) {
            moveTaskToBack(true)
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}