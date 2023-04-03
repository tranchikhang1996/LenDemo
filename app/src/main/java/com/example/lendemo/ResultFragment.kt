package com.example.lendemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lendemo.databinding.FragmentResultBinding


class ResultFragment : Fragment() {
    private lateinit var binding: FragmentResultBinding
    private val viewModel: ScannedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.result.observe(viewLifecycleOwner) {
            setResult(it)
        }
        binding.imageView.onTextSelectedListener = {
            binding.textSelected.text = it
        }
    }

    private fun setResult(result: ScannedResult) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root)
        constraintSet.setDimensionRatio(R.id.imageView, "${result.image.width}:${result.image.height}")
        constraintSet.applyTo(binding.root)
        binding.imageView.setImageBitmap(result.image.bitmapInternal)
        binding.imageView.setTextBlocks(result)
    }
}