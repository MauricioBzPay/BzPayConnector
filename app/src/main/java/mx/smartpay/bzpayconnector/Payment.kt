package mx.smartpay.bzpayconnector

data class Payment(
    val messageType: Int  = 1,
    val sourceDevice: String = "90",
    val destinationDevice: String = "01",
    val identifier: String = "13",
    val cardNumber: String? = null,
    val expirationDate: String?,
    val amountInCents: Double,
    val systemNumber: String,
    val transactionReference: String?,
    val numberOfCreditCardTypes: Int,
    val identifierCreditCard: String,
    val prefixFromCreditCard: String,
    val prefixToOfCreditCard: String,
    val surchargeInCents: Double
)
