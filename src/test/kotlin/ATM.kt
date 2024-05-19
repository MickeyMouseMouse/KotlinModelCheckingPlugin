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
            next(false, false, false)
        }
    }

    fun next(attempt1: Boolean, attempt2: Boolean, attempt3: Boolean) {
        when (mode) {
            0 -> mode = 1
            1 -> {
                mode = if (attempt1) 4 else 2
            }
            2 -> {
                mode = if (attempt2) 4 else 3
            }
            3 -> {
                mode = if (attempt3) 4 else 0
            }
            4 -> mode = 5
            5 -> mode = 0
        }
    }
}