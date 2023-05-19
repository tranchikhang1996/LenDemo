package com.example.lendemo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.lendemo.databinding.FragmentResultBinding

class ResultFragment : Fragment() {
    companion object {
        private const val IMG_KEY = "img_path_key"
        fun newInstance(uri: Uri) = ResultFragment().apply {
            arguments = bundleOf(IMG_KEY to uri.toString())
        }
    }

    private lateinit var binding: FragmentResultBinding

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
        arguments?.getString(IMG_KEY)?.takeIf { it.isNotEmpty() }?.let {
            setResult(Uri.parse(it))
        }
        binding.imageView.onTextSelectedListener = {
            binding.textSelected.text = it
        }
    }

    private fun setResult(uri: Uri) {
        binding.imageView.setImage(uri)
    }
}