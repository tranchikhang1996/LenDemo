package com.example.lendemo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lendemo.databinding.FragmentCaptureBinding
import java.io.File

class CaptureFragment : Fragment() {

    private lateinit var binding: FragmentCaptureBinding

    @Volatile
    private var imageCapture: ImageCapture? = null
    private lateinit var orientationEventListener: OrientationEventListener
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var pickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val multiplePermissions = ActivityResultContracts.RequestMultiplePermissions()
        permissionLauncher = registerForActivityResult(multiplePermissions) { onPermissionResult() }
        val pickVisualMedia = ActivityResultContracts.PickVisualMedia()
        pickerLauncher =
            registerForActivityResult(pickVisualMedia) { uri -> uri?.let { onImageCaptured(it) } }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.capture.setOnClickListener { takePhoto() }
        binding.gallery.setOnClickListener {
            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            pickerLauncher.launch(request)
        }
        initOrientationEventListener()
        tryToStartCamera()
    }

    private fun tryToStartCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun onPermissionResult() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions not granted!", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    private fun initOrientationEventListener() {
        orientationEventListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                imageCapture?.targetRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        orientationEventListener.enable()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.viewFinder.display?.rotation ?: Surface.ROTATION_0)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            kotlin.runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val file = File(requireContext().externalCacheDir, "capture.png")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture?.takePicture(outputFileOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri?.let { onImageCaptured(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Saved Image error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun onImageCaptured(uri: Uri) {
        (activity as? MainActivity)?.showResult(uri)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orientationEventListener.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionLauncher.unregister()
        pickerLauncher.unregister()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

}