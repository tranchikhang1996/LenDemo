package com.example.lendemo

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.commit
import com.example.lendemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            supportFragmentManager.commit { replace(R.id.fragment_container, CaptureFragment()) }
        }
    }

    fun showResult(uri: Uri) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, ResultFragment.newInstance(uri))
            addToBackStack(null)
        }
    }
}