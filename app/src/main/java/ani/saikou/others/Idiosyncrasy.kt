
package ani.saikou.others

import android.content.Intent
import android.os.Build
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : Serializable> Intent.getSerializable(key: String, m_class: KClass<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.getSerializableExtra(key, m_class.java)
    else
        this.getSerializableExtra(key) as? T
}