package com.example.magpie

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.magpie.databinding.FragmentImageFilterDemoBinding

class ImageFilterDemoFragment : Fragment() {
    private lateinit var binding: FragmentImageFilterDemoBinding
    private var currentFilter: Filter = Filter.None

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageFilterDemoBinding.inflate(layoutInflater, container, false)

        // Demo image setup
        Glide.with(this)
            .load(DEMO_IMAGE_URL)
            .placeholder(ColorDrawable(Color.LTGRAY))
            .into(binding.demoImageView)

        // Filter radio buttons setup
        binding.filterOptionsRadioGroup.check(R.id.filterNoneButton)
        binding.filterOptionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.filterNoneButton -> Filter.None
                R.id.filterGreyscaleButton -> Filter.Greyscale
                R.id.filterSepiaButton -> Filter.Sepia(amount = 1f)
                R.id.filterInvertButton -> Filter.Invert
                R.id.filterHueRotate90Button -> Filter.HueRotate(degrees = 90f)
                else -> Filter.None
            }
            binding.demoImageView.applyFilter(currentFilter)
        }

        return binding.root
    }
}