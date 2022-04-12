package ani.saikou.tv.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.TvLoginFragmentBinding
import ani.saikou.tv.TVAnimeFragment
import kotlinx.coroutines.*

class TVLoginFragment() : Fragment() {

    lateinit var binding: TvLoginFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TvLoginFragmentBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.text = "Please open Saikou on your phone and login\nOnce logged in go to \"Settings/TV Login\" and introduce this code"
        TVAnimeFragment.shouldReload = true

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                binding.code.text = NetworkTVConnection.getLocalIPHost(requireContext())
            }
        }

        listen()
    }

    override fun onDestroy() {
        NetworkTVConnection.stopListening()
        super.onDestroy()
    }

    private fun listen() {
        NetworkTVConnection.listen(object : NetworkTVConnection.OnTokenReceivedCallback {
            override fun onTokenReceived(token: String?) {
                token?.let {tk ->
                    MainScope().launch {
                        saveToken(tk)
                        TVAnimeFragment.shouldReload = true
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                } ?: run {
                    MainScope().launch {
                        if(NetworkTVConnection.isListening) {
                            Toast.makeText(requireContext(), "Something went wrong, try again", Toast.LENGTH_LONG)
                            listen()
                        }
                    }
                }
            }
        })
    }

    private fun saveToken(token: String) {
        Anilist.token = token
        val filename = "anilistToken"
        requireActivity().openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(token.toByteArray())
        }
    }
}