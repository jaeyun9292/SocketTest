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
                onTestInteraction("A")
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