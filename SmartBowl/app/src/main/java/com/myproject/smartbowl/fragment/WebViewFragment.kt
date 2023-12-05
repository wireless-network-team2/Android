package com.myproject.smartbowl.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = binding.webView

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // IP 주소에 접속
        val ipAddress = "http://10.40.45.28:8081"
        //val ipAddress = "http://naver.com"
        webView.loadUrl(ipAddress)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}