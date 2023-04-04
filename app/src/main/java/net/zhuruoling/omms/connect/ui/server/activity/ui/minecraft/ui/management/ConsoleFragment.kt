package net.zhuruoling.omms.connect.ui.server.activity.ui.minecraft.ui.management

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.*
import net.zhuruoling.omms.client.controller.Controller
import net.zhuruoling.omms.connect.R
import net.zhuruoling.omms.connect.client.Connection
import net.zhuruoling.omms.connect.databinding.FragmentMcConsoleBinding
import net.zhuruoling.omms.connect.ui.util.fromJson
import net.zhuruoling.omms.connect.util.awaitExecute


class ConsoleFragment : Fragment() {

    private var _binding: FragmentMcConsoleBinding? = null
    private val binding get() = _binding!!
    private var consoleConnected = false
    private lateinit var controller: Controller
    private var consoleId = ""
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
    }
    private val externalScope: CoroutineScope = lifecycleScope.plus(coroutineExceptionHandler)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMcConsoleBinding.inflate(inflater, container, false)
        binding.consoleConnectStateButton.setOnClickListener {
            consoleConnected = if (consoleConnected) {
                binding.consoleConnectStateButton.isEnabled = false
                disconnect {
                    setButtonState(false)
                }
                false
            } else {
                binding.consoleConnectStateButton.isEnabled = false
                connect {
                    setButtonState(true)
                    externalScope.launch {
                        this@ConsoleFragment.binding.mcOutputText.text = ""
                    }
                }
                true
            }
        }
        binding.send.setOnClickListener {
            if (consoleConnected){
                val line = binding.consoleCommandText.text.toString()
                consoleInput(line)
                binding.consoleCommandText.setText("")
            }
        }
        val data = requireActivity().intent.getStringExtra("data")!!
        controller = fromJson(data, Controller::class.java)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (consoleConnected) disconnect { }
        _binding = null
    }

    private fun setButtonState(state: Boolean) {
        externalScope.launch(Dispatchers.Main) {
            val button = binding.consoleConnectStateButton
            if (state) {//connected
                button.setIconResource(R.drawable.baseline_link_off_24)
                button.setText(R.string.label_disconnect_console)
            } else {//disconnected
                button.setIconResource(R.drawable.baseline_link_24)
                button.setText(R.string.label_connect_console)
            }
            binding.consoleConnectStateButton.isEnabled = true
        }
    }

    private fun connect(callback: () -> Unit) {
        if (Connection.getClientSession().isActive) {
            externalScope.launch(Dispatchers.IO) {
                awaitExecute { latch ->
                    Connection.getClientSession().setOnPermissionDeniedCallback {
                        binding.mcOutputText.setText(R.string.error_permission_denied)
                        latch.countDown()
                        Connection.getClientSession().setOnPermissionDeniedCallback(null)
                    }
                    Connection.getClientSession().startControllerConsole(controller.name, {//launched
                        consoleId = it.a
                        callback()
                        latch.countDown()
                    }, {//log recv
                        print(it.b)
                    }, {//controller not exist
                        binding.mcOutputText.setText(R.string.error_permission_denied)
                        latch.countDown()
                    }, {//console already started
                        binding.mcOutputText.setText(R.string.hint_console_exists)
                        latch.countDown()
                    })
                }
            }
        }

    }

    private fun disconnect(callback: () -> Unit) {
        if (consoleId.isEmpty())return
        externalScope.launch(Dispatchers.IO) {
            awaitExecute { latch ->
                Connection.getClientSession().stopControllerConsole(consoleId, {
                    print(requireContext().getText(R.string.hint_console_stopped))
                    latch.countDown()
                }, {
                    binding.mcOutputText.setText(R.string.hint_console_not_exist)
                    latch.countDown()
                })
            }
            callback()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun print(line: String) {
        externalScope.launch(Dispatchers.Main) {
            binding.mcOutputText.text = binding.mcOutputText.text.toString() + "\n$line"
            scrollToEnd()
        }
    }

    private fun consoleInput(line: String) {
        externalScope.launch(Dispatchers.IO) {
            awaitExecute { latch ->
                Connection.getClientSession().controllerConsoleInput(
                    consoleId, line, {
                        binding.mcOutputText.setText(R.string.hint_console_not_exist)
                        latch.countDown()
                    }, {
                        print("> $line")
                        latch.countDown()
                    })
            }
           scrollToEnd()
        }
    }

    private fun scrollToEnd(){
        externalScope.launch (Dispatchers.Main){
            binding.scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}