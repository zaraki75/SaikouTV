package ani.saikou.manga.mangareader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.saikou.BottomSheetDialogFragment
import ani.saikou.databinding.BottomSheetCurrentReaderSettingsBinding
import ani.saikou.settings.CurrentReaderSettings
import ani.saikou.settings.CurrentReaderSettings.Directions

class ReaderSettingsDialogFragment(val activity: MangaReaderActivity) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetCurrentReaderSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCurrentReaderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = activity.settings.default

        binding.readerDirectionText.text = settings.direction.toString()
        binding.readerDirection.rotation = 90f * (settings.direction.ordinal)
        binding.readerDirection.setOnClickListener {
            settings.direction = Directions[settings.direction.ordinal + 1] ?: Directions.TOP_TO_BOTTOM
            binding.readerDirectionText.text = settings.direction.toString()
            binding.readerDirection.rotation = 90f * (settings.direction.ordinal)
            activity.applySettings()
        }

        val list = listOf(
            binding.readerPaged,
            binding.readerContinuousPaged,
            binding.readerContinuous
        )

        binding.readerPadding.isEnabled = settings.layout.ordinal!=0
        fun paddingAvailable(enable:Boolean){
            binding.readerPadding.isEnabled = enable
        }

        binding.readerPadding.isChecked = settings.padding
        binding.readerPadding.setOnCheckedChangeListener { _,isChecked ->
            settings.padding = isChecked
            activity.applySettings()
        }

        binding.readerLayoutText.text = settings.layout.toString()
        var selected = list[settings.layout.ordinal]
        selected.alpha = 1f

        list.forEachIndexed { index , imageButton ->
            imageButton.setOnClickListener {
                selected.alpha = 0.33f
                selected = imageButton
                selected.alpha = 1f
                settings.layout = CurrentReaderSettings.Layouts[index]?:CurrentReaderSettings.Layouts.CONTINUOUS
                binding.readerLayoutText.text = settings.layout.toString()
                activity.applySettings()
                paddingAvailable(settings.layout.ordinal!=0)
            }
        }

        binding.readerKeepScreenOn.isChecked = settings.keepScreenOn
        binding.readerKeepScreenOn.setOnCheckedChangeListener { _,isChecked ->
            settings.keepScreenOn = isChecked
            activity.applySettings()
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}