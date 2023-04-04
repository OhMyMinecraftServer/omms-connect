package net.zhuruoling.omms.connect.ui.server.activity.ui.system.status

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.*
import net.zhuruoling.omms.client.system.SystemInfo
import net.zhuruoling.omms.client.util.Result
import net.zhuruoling.omms.connect.R
import net.zhuruoling.omms.connect.client.Connection
import net.zhuruoling.omms.connect.databinding.FragmentOsStatusBinding
import net.zhuruoling.omms.connect.ui.server.activity.ui.system.view.OsStorageStatusEntryView
import net.zhuruoling.omms.connect.ui.util.Assets
import net.zhuruoling.omms.connect.ui.util.formatResString
import net.zhuruoling.omms.connect.ui.util.getSystemType
import net.zhuruoling.omms.connect.ui.util.showErrorDialog
import net.zhuruoling.omms.connect.util.awaitExecute
import kotlin.math.ceil
class StatusFragment : Fragment() {

    private var _binding: FragmentOsStatusBinding? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
    }
    private val externalScope: CoroutineScope = lifecycleScope.plus(coroutineExceptionHandler)
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val binding get() = _binding!!
    private lateinit var systemInfo: SystemInfo
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentOsStatusBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val original = requireActivity().intent.getStringExtra("data")
        systemInfo = GsonUtils.fromJson(original, SystemInfo::class.java)
        refreshSystemInfo(false)
        binding.osStatusScrollRefresh.setOnRefreshListener {
            refreshSystemInfo(true)
        }
        return root
    }

    @SuppressLint("SetTextI18n")
    private fun refreshSystemInfo(fetch: Boolean) {
        if (!Connection.isConnected) {
            showErrorDialog("Disconnected from Server.", requireContext())
            return
        }
        externalScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                binding.osStatusScrollRefresh.isRefreshing = true
                binding.osMemoryUsage.isIndeterminate = true
                binding.osSwapUsage.isIndeterminate = true
            }
            if (fetch) {
                try {
                    awaitExecute {latch ->
                        Connection.getClientSession().fetchSystemInfoFromServer{
                            systemInfo = Connection.getClientSession().systemInfo
                            latch.countDown()
                        }
                    }
                }catch (e:java.lang.Exception){
                    showErrorDialog(formatResString(R.string.error_system_info_fetch_error,e.toString(), context = requireContext()), requireContext())
                }
            }
            launch(Dispatchers.Main) {
                ensureActive()
                val icon = Assets.getServerIcon(getSystemType(systemInfo.osName), requireActivity())
                binding.osIcon.setImageDrawable(icon)
                binding.osStatusTitle.text = systemInfo.networkInfo.hostName
                binding.osStatusScrollRefresh.isRefreshing = false

                binding.osMemoryUsage.isIndeterminate = false
                val memoryUsage =
                    ceil((systemInfo.memoryInfo.memoryUsed.toDouble() / systemInfo.memoryInfo.memoryTotal.toDouble()) * 100).toInt()
                binding.osMemoryUsage.progress = memoryUsage
                val totalSpaceMemInGB = String.format("%.1f GB",systemInfo.memoryInfo.memoryTotal.toDouble() / 1024.0 / 1024.0 / 1024.0)
                val usedSpaceMemInGB = String.format("%.1f GB",systemInfo.memoryInfo.memoryUsed.toDouble() / 1024.0 / 1024.0 / 1024.0)
                binding.osMemoryText.text = "$usedSpaceMemInGB/$totalSpaceMemInGB\n$memoryUsage%"

                binding.osSwapUsage.isIndeterminate = false
                val swapUsage =
                    ceil((systemInfo.memoryInfo.swapUsed.toDouble() / systemInfo.memoryInfo.swapTotal.toDouble()) * 100).toInt()
                binding.osSwapUsage.progress = swapUsage
                val totalSpaceSwpInGB = String.format("%.1f GB",systemInfo.memoryInfo.swapTotal.toDouble() / 1024.0 / 1024.0 / 1024.0)
                val usedSpaceSwpInGB = String.format("%.1f GB",systemInfo.memoryInfo.swapUsed.toDouble() / 1024.0 / 1024.0 / 1024.0)
                binding.osSwapText.text = "$usedSpaceSwpInGB/$totalSpaceSwpInGB\n$swapUsage%"

                if (systemInfo.processorInfo.cpuLoadAvg == -1.0){
                    binding.osLoadIndicator.progress = 0
                    binding.osLoadAvgText.setText(R.string.unavailable)
                    //5.38 / 8
                }else{
                    val progress = ceil((systemInfo.processorInfo.cpuLoadAvg * 100) / systemInfo.processorInfo.logicalProcessorCount).toInt()
                    binding.osLoadIndicator.progress = progress
                    binding.osLoadAvgText.text = String.format("%.2f",systemInfo.processorInfo.cpuLoadAvg)
                }

                binding.osStorageInfo.removeAllViews()
                systemInfo.fileSystemInfo.fileSystemList.forEach{
                    val view = OsStorageStatusEntryView(requireContext())
                    view.loadFilesystemData(it)
                    this@StatusFragment.binding.osStorageInfo.addView(view)
                }


            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}