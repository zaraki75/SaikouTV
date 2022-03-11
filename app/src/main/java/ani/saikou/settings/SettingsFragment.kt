package ani.saikou.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ani.saikou.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}