package net.zhuruoling.omms.connect.ui.whitelist.view

import android.content.Context
import net.zhuruoling.omms.connect.client.Connection.getClientSession
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.TextView
import net.zhuruoling.omms.connect.ui.whitelist.activity.WhitelistEditActivity
import android.view.LayoutInflater
import net.zhuruoling.omms.connect.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import net.zhuruoling.omms.client.util.Result
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.*

class WhitelistPlayerView : ConstraintLayout {
    private lateinit var playerNameText: TextView
    private lateinit var fromWhitelist: String
    private lateinit var activity: WhitelistEditActivity
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
        Log.e("wdnmd","FUCK", e)
    }

    private lateinit var externalScope: CoroutineScope

    constructor(context: Context) : super(context) {
        LayoutInflater.from(context).inflate(R.layout.whitelist_player_view, this)

        setOnClickListener { view: View -> displayActions(view) }
        playerNameText = findViewById(R.id.playerNameText)
        setOnClickListener { view: View -> displayActions(view) }
    }

    private fun displayActions(view: View) {
        val dialog = MaterialAlertDialogBuilder(activity)
            .setCancelable(true)
            .setTitle("Confirm")
            .setMessage(
                String.format(
                    "Are you sure to remove this player?\n player:%s\n whitelist: %s",
                    playerNameText.text,
                    fromWhitelist
                )
            )
            .setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                externalScope.launch(Dispatchers.IO) {
                    val playerName = playerNameText.text.toString()
                    val session = getClientSession()

                    val result = session.removeFromWhitelist(fromWhitelist, playerName)
                    if (result != Result.OK) {
                        MaterialAlertDialogBuilder(activity)
                            .setPositiveButton("Ok", null)
                            .setTitle("Fail")
                            .setMessage(
                                String.format(
                                    "Failed to remove %s \n reason:%s",
                                    playerName,
                                    result.name
                                )
                            ).show()

                    } else {
                        this.launch (Dispatchers.Main){
                            MaterialAlertDialogBuilder(activity)
                                .setPositiveButton("Ok", null)
                                .setTitle("Success")
                                .setMessage(
                                    String.format(
                                        "Successfully to removed %s",
                                        playerName,
                                    )
                                ).show()
                            activity.requireRefresh = true
                            activity.removePlayer(playerName)
                            activity.refreshPlayerList()
                        }
                    }
                }
            }
            .setNegativeButton("No"){ _: DialogInterface?, _: Int ->

            }
            .create()
        dialog.show()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.whitelist_player_view, this)
        setOnClickListener { view: View -> displayActions(view) }
    }

    fun setAttribute(
        playerName: String,
        fromWhitelist: String,
        activity: WhitelistEditActivity
    ): WhitelistPlayerView {
        playerNameText!!.text = playerName
        this.fromWhitelist = fromWhitelist
        this.activity = activity
        externalScope = activity.lifecycleScope.plus(coroutineExceptionHandler)
        return this
    }
}