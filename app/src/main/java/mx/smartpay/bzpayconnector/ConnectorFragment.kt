package mx.smartpay.bzpayconnector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import com.hoho.android.usbserial.BuildConfig
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import mx.smartpay.bzpayconnector.databinding.FragmentConnectorBinding
import java.io.IOException
import java.lang.Exception


class ConnectorFragment : Fragment(), SerialInputOutputManager.Listener {

    enum class UsbPermission {
        Unknown, Granted, Denied
    }
    private var _binding: FragmentConnectorBinding? = null
    private val binding get() = _binding!!
    private var broadcastReceiver: BroadcastReceiver? = null
    private val INTENT_ACTION_GRANT_USB: String = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB"
    private var usbPermission: UsbPermission = UsbPermission.Unknown
    private var mainLooper: Handler? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    private var portNum: Int = 0
    private var baudRate: Int = 0
    private var deviceId: Int = 0
    private var connected: Boolean = false


    init {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (INTENT_ACTION_GRANT_USB == intent.action) {
                    usbPermission = if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED,
                            false
                        )
                    ) UsbPermission.Granted else UsbPermission.Denied
                }
            }

        }
        mainLooper = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectorBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
        binding.receiveText.setTextColor(resources.getColor(R.color.black))
        binding.receiveText.movementMethod = ScrollingMovementMethod.getInstance()
        //read()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val checkboxListen: CheckBox = view.findViewById(R.id.checkbox_listen)
        checkboxListen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                autoDetectDevice()
            } else {
                showToast("disconnected")
                disconnect()
                binding.receiveText.text = ""

            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(broadcastReceiver, IntentFilter(INTENT_ACTION_GRANT_USB))
        if (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted) mainLooper?.post(
            Runnable { this.autoDetectDevice() })
    }

    override fun onPause() {
        if (connected) {
            showToast("disconnected")
            disconnect()
        }
        activity?.unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    override fun onNewData(data: ByteArray?) {
        mainLooper?.post(Runnable {
            if (data != null) {
                receive(data)
            }
        })
    }

    override fun onRunError(e: Exception) {
        mainLooper?.post(Runnable {
            showToast("connection lost: ${e.message}")
            disconnect()
        })
    }

    private fun autoDetectDevice() {
        val usbManager = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in usbManager.deviceList.values) {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                ?: CustomProver.getCustomProver().probeDevice(device)
            if (driver != null) {
                deviceId = device.deviceId
                portNum = 0
                baudRate = 19200
                connect()
                return
            }
        }
    }

    private fun connect() {
        var device: UsbDevice? = null
        val usbManager = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager
        for (v in usbManager.deviceList.values) if (v.deviceId == deviceId) device = v
        if (device == null) {
            showToast("connection failed: device not found")
            return
        }
        var driver: UsbSerialDriver = UsbSerialProber.getDefaultProber().probeDevice(device)
            ?: CustomProver.getCustomProver().probeDevice(device) ?: run {
                showToast("connection failed: no driver for device")
                return
            }

        if (driver.ports.size <= portNum) {
            showToast("connection failed: not enough ports at device")
            return
        }

        usbSerialPort = driver.ports[portNum]
        val usbConnection = usbManager.openDevice(driver.device)
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.device)) {

                showToast("connection failed: permission denied")
            } else {

                showToast("connection failed: open failed")
            }
            return
        }

        try {
            usbSerialPort?.open(usbConnection)
            usbSerialPort?.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)

            // Habilitar el IoManager
            usbIoManager = SerialInputOutputManager(usbSerialPort, this)
            usbIoManager?.start()
            showToast("connected")
            connected = true
        } catch (e: Exception) {
            showToast("connection failed: ${e.message}")
            disconnect()
        }
    }


    private fun disconnect() {
        connected = false
        if (usbIoManager != null) {
            usbIoManager!!.setListener(null)
            usbIoManager!!.stop()
        }
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (ignored: IOException) {
        }
        usbSerialPort = null
    }

    private fun receive(data: ByteArray) {
        val spn = SpannableStringBuilder()

        Log.d("SwitchUsbFragment", " datos: ${data.size} ")

        spn.append(
            """receive ${data.size} bytes
"""
        )
        if (data.size > 0) spn.append(TextUtil.dumpHexString(data)).append("\n")
        binding.receiveText.append(spn)
    }

    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun status(str: String) {
        val spn = SpannableStringBuilder(
            """
              $str
              
              """.trimIndent()
        )
        spn.setSpan(
            ForegroundColorSpan(getResources().getColor(R.color.black)),
            0,
            spn.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.receiveText.append(spn)
    }

    /*private fun read() {
        if (!connected) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val buffer = ByteArray(8192)
            val len: Int? = usbSerialPort?.read(buffer, 2000)
            len?.let { Arrays.copyOf(buffer, it) }?.let { receive(it) }
        } catch (e: IOException) {
            status("connection lost: " + e.message)
            disconnect()
        }
    }*/
}