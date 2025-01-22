package mx.smartpay.bzpayconnector

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialProber

class CustomProver {
    companion object {
        fun getCustomProver(): UsbSerialProber {
            var customTable = ProbeTable()
            customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver::class.java)
            return UsbSerialProber(customTable)
        }
    }
}