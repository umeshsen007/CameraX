package com.example.camerax

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min


@Composable
fun CameraPreviewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current


    val lensFacing = remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val localConfiguration = LocalConfiguration.current
    val density = LocalDensity.current

    val minSizeReq = getTargetResolutionSize(localConfiguration).dp
    val minSizeInPx = with(density) { minSizeReq.roundToPx() }
    val previewView = remember {
        PreviewView(context)
    }

    val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(ResolutionStrategy(Size(1080, 1080), ResolutionStrategy.FALLBACK_RULE_NONE))
        .build()

    val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing.intValue).build()

    val imageCapture = remember {
        ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)
        preview.surfaceProvider = previewView.surfaceProvider
    }

    CameraView(previewView, imageCapture, context, minSizeInPx)
}

@Composable
private fun CameraView(previewView: PreviewView, imageCapture: ImageCapture, context: Context, minSizeInPx: Int) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(0.8f)
        ) {
            Box(modifier = Modifier) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.aspectRatio(1f)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_profile_male_camera),
                    contentDescription = "user image",
                    modifier = Modifier
                        .aspectRatio(1f),
                    alignment = Alignment.Center
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.ic_camera_icon),
            contentDescription = "",
            modifier = Modifier
                .size(50.dp)
                .clickable {
                    captureImage(imageCapture, context)
                }
        )
    }
}


private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun captureImage(imageCapture: ImageCapture, context: Context) {
    val name = "CameraxImage.jpeg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                println("Successs")
                Toast.makeText(context, name, Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                println("Failed $exception")
            }
        })
}

private fun getTargetResolutionSize(localConfiguration: Configuration): Int {

    val widthInDp = localConfiguration.screenWidthDp
    val heightInDp = localConfiguration.screenHeightDp

    return min(widthInDp, heightInDp)
}