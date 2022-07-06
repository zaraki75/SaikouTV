package ani.saikou.anilist.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.text.DateFormatSymbols
import java.util.*

data class FuzzyDate(
    @SerializedName("year") val year: Int? = null,
    @SerializedName("month") val month: Int? = null,
    @SerializedName("day") val day: Int? = null,
) : Serializable {
    override fun toString(): String {
        val a = if (month != null) DateFormatSymbols().months[month - 1] else ""
        return (if (day != null) "$day " else "") + a + (if (year != null) ", $year" else "")
    }

    fun getToday(): FuzzyDate {
        val cal = Calendar.getInstance()
        return FuzzyDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun toVariableString(): String {
        return ("{"
                + (if (year != null) "year:$year" else "")
                + (if (month != null) ",month:$month" else "")
                + (if (day != null) ",day:$day" else "")
                + "}")
    }
}