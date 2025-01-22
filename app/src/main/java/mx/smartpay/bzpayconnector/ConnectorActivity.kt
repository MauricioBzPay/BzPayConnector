package mx.smartpay.bzpayconnector

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import mx.smartpay.bzpayconnector.databinding.ActivityConnectorBinding

class ConnectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            if (currentFragment is ConnectorFragment) {
                currentFragment.showToast("USB device detected")
            }
        }
    }
}