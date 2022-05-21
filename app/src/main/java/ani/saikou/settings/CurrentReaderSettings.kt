package ani.saikou.settings

import java.io.Serializable

data class CurrentReaderSettings(
    var direction: Directions = Directions.TOP_TO_BOTTOM,
    var layout: Layouts = Layouts.CONTINUOUS,
    var dualPageMode: DualPageModes = DualPageModes.Automatic,
    var padding: Boolean = true,
    var horizontalScrollBar: Boolean = true,
    var keepScreenOn: Boolean = false,
) : Serializable {

    enum class Directions {
        TOP_TO_BOTTOM, LEFT_TO_RIGHT, BOTTOM_TO_TOP, RIGHT_TO_LEFT;

        override fun toString(): String {
            return when (super.ordinal) {
                TOP_TO_BOTTOM.ordinal -> "Top to Bottom"
                LEFT_TO_RIGHT.ordinal -> "Left to Right"
                BOTTOM_TO_TOP.ordinal -> "Bottom to Top"
                RIGHT_TO_LEFT.ordinal -> "Right to Left"
                else                  -> "Wha"
            }
        }

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class Layouts {
        PAGED, CONTINUOUS_PAGED, CONTINUOUS;

        override fun toString(): String {
            return when (super.ordinal) {
                PAGED.ordinal            -> "Paged"
                CONTINUOUS_PAGED.ordinal -> "Continuous Paged"
                CONTINUOUS.ordinal       -> "Continuous"
                else                     -> "Wha"
            }
        }

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class DualPageModes {
        No, Automatic, Force;

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }
}

