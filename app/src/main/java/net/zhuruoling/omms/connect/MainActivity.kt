package net.zhuruoling.omms.connect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import net.zhuruoling.omms.connect.client.Connection
import net.zhuruoling.omms.connect.client.Connection.Result
import net.zhuruoling.omms.connect.client.Response
import net.zhuruoling.omms.connect.databinding.ActivityMainBinding
import net.zhuruoling.omms.connect.storage.PreferencesStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
        alertDialog.dismiss()
    }

    private val externalScope: CoroutineScope = lifecycleScope.plus(coroutineExceptionHandler)
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private lateinit var alertDialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        alertDialog = MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .setTitle("Loading")
            .setMessage(R.string.working)
            .create()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val button = binding.buttonLogin
        binding.remeberCodeCheckbox.isClickable = false
        binding.remeberServerCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.remeberCodeCheckbox.isClickable = isChecked
            if (!isChecked and binding.remeberCodeCheckbox.isChecked){
                binding.remeberCodeCheckbox.isChecked = false
            }
        }
        val preferencesStorage = PreferencesStorage.withContext(this, "login")
        button.setOnClickListener {

            val ip: String = binding.textIpaddr.text.toString()
            val port = binding.textPort.text.toString()
            val code = binding.textCode.text.toString()
            if (ip.isEmpty() or port.isEmpty() or code.isEmpty()) {
                ToastUtils.showLong(R.string.empty)
                return@setOnClickListener
            }
            if (binding.remeberServerCheckbox.isChecked){
                preferencesStorage.putString("server_ip",ip).putString("server_port",port)
            }
            else{
                preferencesStorage.clear()
            }
            if (binding.remeberCodeCheckbox.isChecked){
                preferencesStorage.putString("server_code",code)
            }

            preferencesStorage.commit()
            login(ip, Integer.valueOf(port), Integer.valueOf(code))
            alertDialog.show()
            }
        if (preferencesStorage.contains("server_ip") and preferencesStorage.contains("server_port")){
            val ip = preferencesStorage.getString("server_ip","")
            val port = preferencesStorage.getString("server_port","")
            binding.textIpaddr.text = SpannableStringBuilder(ip)
            binding.textPort.text = SpannableStringBuilder(port)
            binding.remeberServerCheckbox.isChecked = true
        }
        if (preferencesStorage.contains("server_code")){
            val code = preferencesStorage.getString("server_code","")
            binding.textCode.text = SpannableStringBuilder(code)
            binding.remeberCodeCheckbox.isChecked = true
        }
        binding.settingButton.setOnClickListener {
            ActivityUtils.startActivity(SettingsActivity::class.java)
        }
        binding.ommsIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/OhMyMinecraftServer"))
            startActivity(intent)
        }
    }

    private fun login(ip: String, port: Int, code: Int) {
        externalScope.launch(defaultDispatcher) {
            ensureActive()
            when (val result = Connection.init(ip, port, code)) {
                is Result.Success<Response> -> {
                    ToastUtils.showLong(R.string.success)
                    startActivity(Intent(this@MainActivity, SessionActivity::class.java))
                    runOnUiThread {
                        alertDialog.dismiss()
                    }
                }
                else -> {
                    runOnUiThread {
                        alertDialog.dismiss()
                        val dialog = MaterialAlertDialogBuilder(this@MainActivity)
                            .setCancelable(true)
                            .setTitle("Loading")
                            .setMessage(String.format("Cannot connect to server, reason %s", (result as Result.Error).exception.toString()))
                            .create()
                        dialog.show()
                        ToastUtils.showLong(R.string.fail)
                    }

                }
            }
        }
    }
}