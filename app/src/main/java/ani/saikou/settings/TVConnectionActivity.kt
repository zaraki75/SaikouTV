package ani.saikou.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.FragmentTvConnectionBinding
import ani.saikou.toastString
import ani.saikou.tv.login.NetworkTVConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVConnectionActivity: AppCompatActivity() {

    lateinit var binding: FragmentTvConnectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTvConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener {
            val fieldText: String? = binding.ipField.text.toString()
            if(fieldText != null && fieldText.isNotEmpty()) {
                binding.progressView.visibility = View.VISIBLE
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        NetworkTVConnection.connect(
                            this@TVConnectionActivity,
                            Anilist.token!!,
                            fieldText,
                            object : NetworkTVConnection.OnTokenSentCallback {
                                override fun onTokenSent(sent: Boolean) {
                                    if (sent) {
                                        MainScope().launch {
                                            onBackPressed()
                                        }
                                    } else {
                                        MainScope().launch {
                                            binding.progressView.visibility = View.GONE
                                            toastString("Something went wrong, try again")
                                        }
                                    }
                                }
                            })
                    }
                }
            } else {
                toastString("Please input the number shown on TV")
            }
        }
    }
}