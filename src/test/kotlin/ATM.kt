import org.example.kotlinmodelcheckingplugin.annotations.*

@LTL("G ((mode=0) -> X(mode!=5))")
class ATM {
    // mode 0 = waiting new client
    // mode 1 = pincode check 1
    // mode 2 = pincode check 2
    // mode 3 = pincode check 3
    // mode 4 = amount check
    // mode 5 = withdraw
    @StateVar
    private var mode: Int = 0

    fun main() {
        for (i in 0..5) {
            next(pinAttempt1 = false, pinAttempt2 = true, pinAttempt3 = false, amountCheck = true)
        }
        //next(pinAttempt1 = false, pinAttempt2 = true, pinAttempt3 = false, amountCheck = true)
        //next(pinAttempt1 = false, pinAttempt2 = true, pinAttempt3 = false, amountCheck = true)
    }

    fun next(pinAttempt1: Boolean, pinAttempt2: Boolean, pinAttempt3: Boolean, amountCheck: Boolean) {
        when (mode) {
            0 -> { // "waiting new client"[0] -> "pincode check 1"[1]
                mode = 1
            }
            1 -> { // "pincode check 1"[1] -> "amount check"[4] OR "pincode check 2"[2]
                mode = if (pinAttempt1) 4 else 2
            }
            2 -> { // "pincode check 2"[2] -> "amount check"[4] OR "pincode check 3"[3]
                mode = if (pinAttempt2) 4 else 3
            }
            3 -> { // "pincode check 3"[3] -> "amount check"[4] OR "waiting new client"[0]
                mode = if (pinAttempt3) 4 else 0
            }
            4 -> { // "amount check"[4] -> "withdraw"[5] OR "amount check"[4]
                mode = if (amountCheck) 5 else 4
            }
            5 -> { // "withdraw"[5] -> "waiting new client"[0]
                mode = 0
            }
        }
    }
}