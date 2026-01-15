package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components


import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CameraCapture"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraEvidenceScreen(
    saleId: String,
    totalAmount: Double,
    onPhotoCaptured: (File) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Permission not granted screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permiso de Cámara Requerido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Necesitamos acceso a la cámara para capturar\nel comprobante de pago",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() }
                ) {
                    Text("Conceder Permiso")
                }
            }
        }
        return
    }

    // Camera UI
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera binding failed", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay with info
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Capturar Comprobante",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total de la Venta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Bottom controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enfoca el comprobante de transferencia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Capture button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                capturePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    saleId = saleId,
                                    totalAmount = totalAmount,
                                    onSuccess = { file ->
                                        isCapturing = false
                                        onPhotoCaptured(file)
                                    },
                                    onError = {
                                        isCapturing = false
                                        Log.e(TAG, "Capture failed", it)
                                    }
                                )
                            }
                        },
                        enabled = !isCapturing,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (isCapturing) Icons.Default.HourglassEmpty else Icons.Default.Camera,
                            contentDescription = "Capturar",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    saleId: String,
    totalAmount: Double,
    onSuccess: (File) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(Exception("ImageCapture not initialized"))
        return
    }

    // Archivo temporal en cache
    val photoFile = File(
        context.cacheDir,
        "transfer_voucher_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d(TAG, "Photo captured: ${photoFile.absolutePath}")

                // Agregar watermark con info de la venta
                val markedFile = addWatermark(
                    context = context,
                    originalFile = photoFile,
                    saleId = saleId,
                    totalAmount = totalAmount
                )

                onSuccess(markedFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed", exception)
                onError(exception)
            }
        }
    )
}

private fun addWatermark(
    context: Context,
    originalFile: File,
    saleId: String,
    totalAmount: Double
): File {
    try {
        val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val watermarkText = "ID: $saleId\nMonto: $${String.format("%.2f", totalAmount)}\n$timestamp"
        val lines = watermarkText.split("\n")

        var y = canvas.height - 100f
        lines.reversed().forEach { line ->
            canvas.drawText(line, 30f, y, paint)
            y -= 50f
        }

        // Guardar imagen marcada
        val markedFile = File(
            context.cacheDir,
            "marked_${originalFile.name}"
        )

        FileOutputStream(markedFile).use { out ->
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        // Borrar original
        originalFile.delete()

        return markedFile

    } catch (e: Exception) {
        Log.e(TAG, "Error adding watermark", e)
        return originalFile
    }
}