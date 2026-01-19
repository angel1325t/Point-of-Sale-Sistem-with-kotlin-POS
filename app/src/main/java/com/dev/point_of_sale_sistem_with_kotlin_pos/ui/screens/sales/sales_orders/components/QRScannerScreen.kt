package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components

import android.Manifest
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    title: String = "Escanear QR",
    onQRScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var hasScanned by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            torchEnabled = !torchEnabled
                            cameraControl?.enableTorch(torchEnabled)
                        }
                    ) {
                        Icon(
                            imageVector = if (torchEnabled)
                                Icons.Default.FlashlightOn
                            else
                                Icons.Default.FlashlightOff,
                            contentDescription = "Flash",
                            tint = if (torchEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermission.status.isGranted -> {
                    PermissionRequired(
                        onRequestPermission = {
                            cameraPermission.launchPermissionRequest()
                        }
                    )
                }

                else -> {
                    CameraPreview(
                        onQRScanned = { qrContent ->
                            if (!hasScanned) {
                                hasScanned = true
                                onQRScanned(qrContent)
                            }
                        },
                        onCameraControlReady = { control ->
                            cameraControl = control
                        }
                    )

                    ScannerOverlay()

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "Coloca el código QR dentro del marco",
                                modifier = Modifier.padding(
                                    horizontal = 24.dp,
                                    vertical = 12.dp
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CameraPreview(
    onQRScanned: (String) -> Unit,
    onCameraControlReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        BarcodeScanning.getClient()
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Image Analysis para detectar QR
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(imageProxy, barcodeScanner, onQRScanned)
                        }
                    }

                // Seleccionar cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )

                    onCameraControlReady(camera.cameraControl)
                } catch (e: Exception) {
                    Log.e("QRScanner", "Error binding camera", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    onQRScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT,
                        Barcode.TYPE_URL -> {
                            barcode.rawValue?.let { value ->
                                onQRScanned(value)
                            }
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scanAreaSize = minOf(canvasWidth, canvasHeight) * 0.7f
        val left = (canvasWidth - scanAreaSize) / 2
        val top = (canvasHeight - scanAreaSize) / 2

        // Oscurecer el fondo
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Recortar área de escaneo (transparente)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scanAreaSize, scanAreaSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Marco del área de escaneo
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanAreaSize, scanAreaSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 4.dp.toPx())
        )

        // Esquinas del marco (animadas)
        val cornerLength = 40.dp.toPx()
        val cornerWidth = 6.dp.toPx()
        val cornerColor = Color(0xFF4CAF50)

        // Esquina superior izquierda
        drawLine(
            color = cornerColor,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = cornerWidth
        )

        // Esquina superior derecha
        drawLine(
            color = cornerColor,
            start = Offset(left + scanAreaSize, top),
            end = Offset(left + scanAreaSize - cornerLength, top),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + scanAreaSize, top),
            end = Offset(left + scanAreaSize, top + cornerLength),
            strokeWidth = cornerWidth
        )

        // Esquina inferior izquierda
        drawLine(
            color = cornerColor,
            start = Offset(left, top + scanAreaSize),
            end = Offset(left + cornerLength, top + scanAreaSize),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top + scanAreaSize),
            end = Offset(left, top + scanAreaSize - cornerLength),
            strokeWidth = cornerWidth
        )

        // Esquina inferior derecha
        drawLine(
            color = cornerColor,
            start = Offset(left + scanAreaSize, top + scanAreaSize),
            end = Offset(left + scanAreaSize - cornerLength, top + scanAreaSize),
            strokeWidth = cornerWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(left + scanAreaSize, top + scanAreaSize),
            end = Offset(left + scanAreaSize, top + scanAreaSize - cornerLength),
            strokeWidth = cornerWidth
        )
    }
}

@Composable
private fun PermissionRequired(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashlightOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Permiso de cámara requerido",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Necesitamos acceso a la cámara para escanear códigos QR",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder Permiso")
        }
    }
}