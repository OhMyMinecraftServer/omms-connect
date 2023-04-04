package net.zhuruoling.omms.connect.ui.announcement

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import net.zhuruoling.omms.client.util.Result
import net.zhuruoling.omms.connect.client.Connection
import net.zhuruoling.omms.connect.ui.util.showErrorDialog
import net.zhuruoling.omms.connect.databinding.FragmentAnnouncementBinding
import net.zhuruoling.omms.connect.util.awaitExecute

class AnnouncementFragment : Fragment() {

    private var _binding: FragmentAnnouncementBinding? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        ToastUtils.showLong("Failed connect to server\nreason:$e")
    }
    private val externalScope: CoroutineScope = lifecycleScope.plus(coroutineExceptionHandler)
    private val binding get() = _binding!!
    private lateinit var alertDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnouncementBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.announcementSwipeRefresh.isRefreshing = false
        binding.announcementSwipeRefresh.setOnRefreshListener {
            refresh(false)
        }
        refresh(true)
        return root
    }

    private fun refresh(showDialog: Boolean) {
        if (showDialog) {
            showLoadDialog()
        }
        externalScope.launch(Dispatchers.IO) {
            ensureActive()
            if (Connection.isConnected) {
                try {
                    awaitExecute { latch ->
                        Connection.getClientSession().fetchAnnouncementFromServer {
                            launch(Dispatchers.Main) {
                                binding.announcementList.removeAllViews()
                                val map = it
                                this@AnnouncementFragment.binding.announcementTitle.text = "${map.count()} announcements added to this server."
                                dismissLoadDialog(showDialog)
                                latch.countDown()
                            }
                        }
                    }

                } catch (e: Exception) {
                    showAlertAndDismissDialog("Error in fetching announcement.\n$e", showDialog)
                    return@launch
                }
            } else {
                showAlertAndDismissDialog("Disconnected from server.", showDialog)
                return@launch
            }

        }
    }

    private fun dismissLoadDialog(showDialog: Boolean) {
        if (showDialog) {
            alertDialog.dismiss()
        } else {
            this@AnnouncementFragment.binding.announcementSwipeRefresh.isRefreshing = false
        }
    }

    private fun showAlertAndDismissDialog(info: String, showDialog: Boolean) {
        externalScope.launch(Dispatchers.Main) {
            showErrorDialog(info, requireContext())
            dismissLoadDialog(showDialog)
        }
    }

    private fun showLoadDialog() {
        alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setCancelable(false)
            .setTitle("Loading")
            .setMessage("Please Wait...")
            .create()
        externalScope.launch(Dispatchers.Main) {
            alertDialog.show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}