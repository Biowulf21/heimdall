package com.chooloo.www.koler.ui.call

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.telecom.InCallService.VideoCall
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.chooloo.www.koler.R

@Composable
internal fun CallVideoPanel(call: VideoCall) = key(call) {
    val context = LocalContext.current
    val camera = remember(context) { frontCamera(context) }
    var cameraEnabled by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        cameraEnabled = it
    }
    var showPreview by remember { mutableStateOf(false) }

    LifecycleResumeEffect(call, cameraEnabled, camera) {
        showPreview = cameraEnabled && camera != null &&
            context.checkSelfPermission(CAMERA) == PERMISSION_GRANTED
        if (showPreview) call.setCamera(camera)
        onPauseOrDispose {
            call.setCamera(null)
            showPreview = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoSurface(Modifier.size(176.dp, 132.dp), onSurface = call::setDisplaySurface)
            if (showPreview) {
                VideoSurface(Modifier.size(88.dp, 132.dp), onSurface = call::setPreviewSurface)
            }
        }
        if (camera != null) {
            TextButton(onClick = {
                when {
                    cameraEnabled -> cameraEnabled = false

                    context.checkSelfPermission(CAMERA) == PERMISSION_GRANTED -> {
                        cameraEnabled = true
                    }

                    else -> permission.launch(CAMERA)
                }
            }) {
                val label = if (cameraEnabled) {
                    R.string.contact_camera_off
                } else {
                    R.string.contact_camera_on
                }
                Text(stringResource(label))
            }
        }
    }
}

private fun frontCamera(context: Context): String? = runCatching {
    val manager = context.getSystemService(CameraManager::class.java)
    manager.cameraIdList.firstOrNull {
        val characteristics = manager.getCameraCharacteristics(it)
        val direction = characteristics.get(CameraCharacteristics.LENS_FACING)
        direction == CameraCharacteristics.LENS_FACING_FRONT
    }
}.getOrNull()

@Composable
private fun VideoSurface(modifier: Modifier, onSurface: (Surface?) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) = onSurface(holder.surface)
                    override fun surfaceDestroyed(holder: SurfaceHolder) = onSurface(null)
                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int,
                    ) = Unit
                })
            }
        },
    )
}
