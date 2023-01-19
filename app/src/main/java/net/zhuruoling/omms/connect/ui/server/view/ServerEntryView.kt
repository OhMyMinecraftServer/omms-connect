package net.zhuruoling.omms.connect.ui.server.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.GsonUtils
import net.zhuruoling.omms.client.controller.Controller
import net.zhuruoling.omms.client.system.SystemInfo
import net.zhuruoling.omms.connect.R
import net.zhuruoling.omms.connect.ui.server.activity.ui.minecraft.MinecraftServerControlActivity
import net.zhuruoling.omms.connect.ui.server.activity.ui.system.ServerOSControlActivity
import net.zhuruoling.omms.connect.ui.util.Assets
import net.zhuruoling.omms.connect.ui.util.ServerEntryType
import java.lang.Exception

class ServerEntryView : ConstraintLayout {

    private var imageView: ImageView
    private var serverNameTextView: TextView
    private var serverIntroTextView: TextView
    private var controller: Controller? = null
    private var systemInfo: SystemInfo? = null
    private var serverEntryType = ServerEntryType.UNDEFINED
    private var entryType = ""

    constructor(context: Context) : super(context) {
        LayoutInflater.from(context).inflate(R.layout.server_entry_view, this)
        this.imageView = findViewById(R.id.server_image)
        serverNameTextView = findViewById(R.id.server_name_text)
        serverIntroTextView = findViewById(R.id.server_intro_text)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.server_entry_view, this)
        this.imageView = findViewById(R.id.server_image)
        serverNameTextView = findViewById(R.id.server_name_text)
        serverIntroTextView = findViewById(R.id.server_intro_text)
    }

    fun setValue(name: String, introText: String, type: String, parent: Activity): ServerEntryView {
        try {
            this.serverNameTextView.text = name
            this.serverIntroTextView.text = introText
            this.entryType = type
            this.imageView.setImageDrawable(Assets.getServerIcon(type, parent))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    fun withController(controller: Controller): ServerEntryView {

        this.controller = controller
        this.serverEntryType = ServerEntryType.MINECRAFT
        return this
    }

    fun withSystemInfo(systemInfo: SystemInfo): ServerEntryView {
        this.systemInfo = systemInfo
        serverEntryType = ServerEntryType.OS
        return this
    }

    fun prepare(parent: Fragment): ServerEntryView {
        this.setOnClickListener(
            if (serverEntryType == ServerEntryType.OS) OnClickListener {
                parent.startActivity(
                    Intent(
                        parent.activity,
                        ServerOSControlActivity::class.java
                    ).putExtra("data", GsonUtils.toJson(systemInfo))
                        .putExtra("system_type", entryType)
                )
            }
            else OnClickListener {
                parent.startActivity(
                    Intent(
                        parent.activity,
                        MinecraftServerControlActivity::class.java
                    ).putExtra("data", GsonUtils.toJson(controller))
                        .putExtra("server_type", entryType)
                )
            }
        )

        return this
    }


}