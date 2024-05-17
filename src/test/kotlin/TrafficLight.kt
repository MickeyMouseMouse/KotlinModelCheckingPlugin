import org.example.kotlinmodelcheckingplugin.annotations.StateVar
import org.example.kotlinmodelcheckingplugin.annotations.LTL
import org.example.kotlinmodelcheckingplugin.annotations.CTL


@LTL("G (!(red = TRUE & yellow = TRUE & green = TRUE))")
@CTL("G mode < 5")
class TrafficLight {
    @StateVar
    private var mode: Int // 0 = off; 1 = stop; 2 = get ready to start; 3 = start; 4 = get ready to stop

    private var red: Boolean

    private var yellow: Boolean = false

    private var green: Boolean = false

    init {
        mode = 0
        red = false
    }
    
    fun main() {
        enable()
        for (i in 1..4) {
            switch()
        }
        disable()
    }

    fun enable() {
        mode = 1
        update()
    }

    fun disable() {
        mode = 0
        update()
    }

    fun switch() {
        when (mode) {
            1 -> mode = 2
            2 -> mode = 3
            3 -> mode = 4
            4 -> mode = 1
        }
        update()
    }

    private fun update() {
        when (mode) {
            0 -> { // off
                red = false
                yellow = false
                green = false
            }
            1 -> { // stop
                red = true
                yellow = false
                green = false
            }
            2 -> { // get ready to start
                red = true
                yellow = true
                green = false
            }
            3 -> { // start
                red = false
                yellow = false
                green = true
            }
            4 -> { // get ready to stop
                red = false
                yellow = true
                green = false
            }
        }
    }
}