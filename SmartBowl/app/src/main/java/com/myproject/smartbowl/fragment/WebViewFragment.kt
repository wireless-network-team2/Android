package com.myproject.smartbowl.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.myproject.smartbowl.databinding.FragmentWebViewBinding
import com.myproject.smartbowl.viewModel.WebViewViewModel

class WebViewFragment : Fragment() {
    private var _binding : FragmentWebViewBinding?= null
    private val binding: FragmentWebViewBinding
        get() = _binding!!

    private lateinit var viewModel: WebViewViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentWebViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}