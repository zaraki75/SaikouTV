package ani.saikou.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.databinding.ActivityPlayerSettingsBinding

class PlayerSettingsActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlayerSettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}