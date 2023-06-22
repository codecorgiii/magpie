package com.example.magpie

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.magpie.databinding.FragmentImageEffectsDemoBinding

class ImageEffectsDemoFragment : Fragment() {
    private lateinit var binding: FragmentImageEffectsDemoBinding
    private var currentEffect: Effect = Effect.BoxBlur

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageEffectsDemoBinding.inflate(inflater, container, false)

        Glide.with(this)
            .load(DEMO_IMAGE_URL)
            .placeholder(ColorDrawable(Color.LTGRAY))
            .into(binding.demoImageView)

        // Filter radio buttons setup
        binding.effectOptionsRadioGroup.check(R.id.effectNoneButton)
        binding.effectOptionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentEffect = when (checkedId) {
                R.id.effectBoxBlurButton -> Effect.BoxBlur
                R.id.effectPixelizeButton -> Effect.Pixelize
                R.id.effectAcrylicButton -> Effect.Acrylic
                else -> Effect.None
            }
            binding.demoImageView.applyEffect(currentEffect)
        }

        return binding.root
    }
}