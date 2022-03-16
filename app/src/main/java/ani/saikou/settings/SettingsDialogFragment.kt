package ani.saikou.settings

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.BottomSheetSettingsBinding


class SettingsDialogFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(Anilist.token!=null){
            binding.settingsLogin.setText(R.string.logout)
            binding.settingsLogin.setOnClickListener {

                Anilist.removeSavedToken(it.context)
                startMainActivity(requireActivity())
            }
            binding.settingsUsername.text = Anilist.username
            binding.settingsUserAvatar.loadImage(Anilist.avatar)
        }else{
            binding.settingsUsername.visibility = View.GONE
            binding.settingsLogin.setText(R.string.login)
            binding.settingsLogin.setOnClickListener {
                Anilist.loginIntent(requireActivity())
            }
        }

        binding.settingsSettings.setSafeOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
        binding.settingsAnilistSettings.setOnClickListener{
            openLinkInBrowser("https://anilist.co/settings/lists")
        }

        binding.settingsDownloads.setSafeOnClickListener {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}