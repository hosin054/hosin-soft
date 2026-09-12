package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    title: String = "قارئ الباركود بالكاميرا",
    subtitle: String = "وجّه الكاميرا نحو الباركود للمسح التلقائي"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera Controls State
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Continuous vs Single scan mode
    var isContinuousMode by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableIntStateOf(0) }

    // Manual input state
    var manualBarcode by remember { mutableStateOf("") }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var showSuccessBadge by remember { mutableStateOf(false) }

    var lastScanTimestamp by remember { mutableLongStateOf(0L) }
    var lastCodeScannedMemory by remember { mutableStateOf("") }

    // Sound / Haptic trigger
    fun triggerSuccessFeedback(code: String) {
        val now = System.currentTimeMillis()
        if (code == lastCodeScannedMemory && (now - lastScanTimestamp) < 1500L) {
            return
        }
        lastCodeScannedMemory = code
        lastScanTimestamp = now

        lastScannedCode = code
        showSuccessBadge = true
        scannedCount++

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }
        } catch (_: Exception) {}

        onBarcodeScanned(code)

        if (!isContinuousMode) {
            coroutineScope.launch {
                delay(400)
                onDismiss()
            }
        } else {
            coroutineScope.launch {
                delay(1200)
                showSuccessBadge = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasCameraPermission && cameraError == null) {
                // Real Camera View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val options = BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(
                                        Barcode.FORMAT_ALL_FORMATS
                                    )
                                    .build()
                                val scanner = BarcodeScanning.getClient(options)
                                val analysisExecutor = Executors.newSingleThreadExecutor()

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                scanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            val rawValue = barcode.rawValue
                                                            if (!rawValue.isNullOrBlank()) {
                                                                triggerSuccessFeedback(rawValue)
                                                                break
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
                                    }

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )

                            } catch (e: Exception) {
                                Log.e("BarcodeScanner", "Camera initialization failed", e)
                                cameraError = "فشل تشغيل الكاميرا: ${e.localizedMessage}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    update = {
                        try {
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        } catch (_: Exception) {}
                    }
                )

                // Viewfinder Reticle Overlay with Animated Scan Laser
                ScannerReticleOverlay(
                    isScanning = !showSuccessBadge,
                    isSuccess = showSuccessBadge
                )

            } else {
                // Permission Denied or Camera Error Fallback UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (cameraError != null) cameraError!! else "مطلوب إذن الكاميرا",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "للبدء في فحص باركود الأصناف تلقائياً بالكاميرا، يرجى منح التطبيق صلاحية الكاميرا.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("منح إذن الكاميرا الآن")
                        }
                    }
                }
            }

            // Top App Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (isContinuousMode && scannedCount > 0) {
                        Text(
                            text = "تم مسح $scannedCount صنف بنجاح",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight toggle
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                isTorchOn = !isTorchOn
                                camera?.cameraControl?.enableTorch(isTorchOn)
                            },
                            modifier = Modifier
                                .background(
                                    if (isTorchOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "الكشاف",
                                tint = Color.White
                            )
                        }

                        // Flip Camera
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = "تبديل الكاميرا",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Manual Barcode Input and Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Feedback badge when scanned
                AnimatedVisibility(
                    visible = showSuccessBadge,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم المسح: $lastScannedCode",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Continuous Scan mode chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isContinuousMode,
                        onClick = { isContinuousMode = false },
                        label = { Text("مسح مفرد وإغلاق") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isContinuousMode,
                        onClick = { isContinuousMode = true },
                        label = { Text("مسح مستمر (سريع)") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Manual Input Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        TextField(
                            value = manualBarcode,
                            onValueChange = { manualBarcode = it },
                            placeholder = { Text("أو أدخل رقم الباركود يدوياً...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        FilledIconButton(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    triggerSuccessFeedback(manualBarcode.trim())
                                    manualBarcode = ""
                                }
                            },
                            enabled = manualBarcode.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "تأكيد")
                        }
                    }
                }

                if (isContinuousMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنهاء المسح (${scannedCount} تم مسحها)")
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerReticleOverlay(isScanning: Boolean, isSuccess: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val cornerColor by animateColorAsState(
        targetValue = if (isSuccess) Color(0xFF10B981) else Color(0xFF38BDF8),
        animationSpec = tween(200),
        label = "corner_color"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val boxWidth = (canvasWidth * 0.75f).coerceIn(240.dp.toPx(), 360.dp.toPx())
        val boxHeight = (boxWidth * 0.75f).coerceAtLeast(180.dp.toPx())

        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2.3f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Surrounding dims
        drawRect(Color.Black.copy(alpha = 0.55f), Offset.Zero, Size(canvasWidth, top))
        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, bottom), Size(canvasWidth, canvasHeight - bottom))
        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, top), Size(left, boxHeight))
        drawRect(Color.Black.copy(alpha = 0.55f), Offset(right, top), Size(canvasWidth - right, boxHeight))

        // Reticle thin border
        drawRoundRect(
            color = if (isSuccess) Color(0xFF10B981).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = if (isSuccess) 2.5.dp.toPx() else 1.5.dp.toPx())
        )

        // 4 glowing corner brackets
        val cornerLength = 28.dp.toPx()
        val cornerStroke = 4.dp.toPx()

        // Top-Left
        drawLine(cornerColor, Offset(left, top + cornerLength), Offset(left, top), cornerStroke)
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)

        // Top-Right
        drawLine(cornerColor, Offset(right - cornerLength, top), Offset(right, top), cornerStroke)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), cornerStroke)

        // Bottom-Left
        drawLine(cornerColor, Offset(left, bottom - cornerLength), Offset(left, bottom), cornerStroke)
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), cornerStroke)

        // Bottom-Right
        drawLine(cornerColor, Offset(right - cornerLength, bottom), Offset(right, bottom), cornerStroke)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), cornerStroke)

        // Animated Laser Scan Line
        if (isScanning) {
            val laserY = top + (boxHeight * laserProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFFEF4444).copy(alpha = 0.8f),
                        Color(0xFFEF4444),
                        Color(0xFFEF4444).copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    startX = left,
                    endX = right
                ),
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(right - 8.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
