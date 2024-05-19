import org.example.kotlinmodelcheckingplugin.annotations.*

@LTL("G ((value1 != 1 & value1 != 2 & value3 != 3) -> X(isOpen = FALSE))")
class Lock {
    @StateVar
    private var value1: Int = 0
    @StateVar
    private var value2: Int = 0
    @StateVar
    private var value3: Int = 0

    private var isOpen: Boolean = false

    private val secret1: Int = 1
    private val secret2: Int = 2
    private val secret3: Int = 3

    fun main() {
        value1 = rotate(value1, 1)
        value2 = rotate(value2, 2)
        value3 = rotate(value3, 3)
        open()
    }

    fun rotate(currentValue: Int, turns: Int) = (currentValue + turns) % 9

    fun open() {
        isOpen = value1 == secret1 && value2 == secret2 && value3 == secret3
    }
}