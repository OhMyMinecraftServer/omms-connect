package net.zhuruoling.omms.connect.ui.whitelist.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BuildCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import net.zhuruoling.omms.client.util.Result
import net.zhuruoling.omms.connect.R
import net.zhuruoling.omms.connect.client.Connection
import net.zhuruoling.omms.connect.databinding.ActivityWhitelistEditBinding
import net.zhuruoling.omms.connect.ui.whitelist.view.WhitelistPlayerView

class WhitelistEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhitelistEditBinding

    var fromWhitelist: String = ""
    var players: ArrayList<String> = arrayListOf()
    var requireRefresh = false
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
        Log.e("wdnmd", "FUCK", e)
    }
    private var externalScope: CoroutineScope = lifecycleScope.plus(coroutineExceptionHandler)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityWhitelistEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fab.setOnClickListener { showActions() }
        fromWhitelist = CacheMemoryUtils.getInstance().get("from_whitelist")
        players = CacheMemoryUtils.getInstance().get("whitelist_content")
        binding.whitelistNameTitle.text = "${players.size} players were added to this whitelist."
        refreshPlayerList()
        if (BuildCompat.isAtLeastT()) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                setResult(114514, Intent().putExtra("requireRefresh", requireRefresh))
                finish()
            }
        }
    }

    fun refreshPlayerList(){
        players.sort()
        externalScope.launch(Dispatchers.Main) {
            this@WhitelistEditActivity.binding.whitelistCompoentContainer.removeAllViews()
            if (players.isNotEmpty()) players.forEach {
                this@WhitelistEditActivity.binding.whitelistCompoentContainer.addView(
                    WhitelistPlayerView(this@WhitelistEditActivity).setAttribute(
                        it,
                        fromWhitelist,
                        this@WhitelistEditActivity
                    )
                )
            }
        }
    }

    fun addPlayer(player: String){
        players.add(player)
        players.sort()
    }

    fun removePlayer(player: String){
        players.remove(player)
        players.sort()
    }


    private fun showActions() {
        val textView = TextInputEditText(this)
        val dialog = MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_baseline_more_horiz_24)
            .setTitle("Add to whitelist")
            .setView(textView)
            .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                val alertDialogBuilder = MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_baseline_more_horiz_24)
                    .setTitle("Working...")
                    .setPositiveButton("OK", null)
                val dialog = alertDialogBuilder.show()
                externalScope.launch(Dispatchers.IO) {
                    val session = Connection.getClientSession()
                    val result = session.addToWhitelist(fromWhitelist, textView.text.toString())
                    this.launch(Dispatchers.Main) {
                        if (result != Result.OK) {
                            dialog.dismiss()
                            MaterialAlertDialogBuilder(this@WhitelistEditActivity)
                                .setIcon(R.drawable.ic_baseline_error_24)
                                .setTitle("Error")
                                .setMessage("Failed to add ${textView.text.toString()} to whitelist, reason: $result")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            dialog.dismiss()
                            MaterialAlertDialogBuilder(this@WhitelistEditActivity)
                                .setIcon(R.drawable.ic_notifications_black_24dp)
                                .setTitle("Success")
                                .setMessage("Added ${textView.text.toString()} to whitelist, reason: $result")
                                .setPositiveButton("OK", null)
                                .show()
                            addPlayer(textView.text.toString())
                            refreshPlayerList()
                        }
                        requireRefresh = true
                    }

                }
            }.setOnCancelListener {
                requireRefresh = false
            }
        dialog.show()
    }

    override fun onDestroy() {
        setResult(114514, Intent().putExtra("requireRefresh", requireRefresh))
        super.onDestroy()
    }


    fun init(from: String, players: ArrayList<String>) {
        this.fromWhitelist = from
        this.players = players
        this.players.sort()
    }

}