package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.models.BodyPart
import com.example.ui.models.BodySystem
import com.example.ui.models.BodyView
import kotlin.math.sqrt

@Composable
fun BodyCanvas(
    selectedView: BodyView,
    selectedSystem: BodySystem,
    activeParts: List<BodyPart>,
    selectedPart: BodyPart?,
    quizActivePart: BodyPart?,
    quizCorrectPartsId: Set<String>,
    quizWrongPartsId: Set<String>,
    onPartClicked: (BodyPart) -> Unit,
    onCanvasTappedAtPercent: (Float, Float) -> Unit,
    editorModeActive: Boolean,
    modifier: Modifier = Modifier
) {
    // Elegant pulsing animation for the hotspots
    val infiniteTransition = rememberInfiniteTransition(label = "hotspot")
    val pulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Glowing cardiac cycle for the heart asset
    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(selectedView, selectedSystem, activeParts) {
                detectTapGestures { offset ->
                    val w = size.width
                    val h = size.height
                    val xPct = offset.x / w
                    val yPct = offset.y / h

                    // Match clicking a point with custom threshold (e.g. 24dp relative coordinates)
                    val clickThresholdPx = 28f
                    var clickedPart: BodyPart? = null
                    var closestDistance = Double.MAX_VALUE

                    for (part in activeParts) {
                        if (part.view == selectedView) {
                            val pxX = part.xPercent * w
                            val pxY = part.yPercent * h
                            val dist = sqrt(
                                ((offset.x - pxX) * (offset.x - pxX) + (offset.y - pxY) * (offset.y - pxY)).toDouble()
                            )
                            if (dist < clickThresholdPx && dist < closestDistance) {
                                clickedPart = part
                                closestDistance = dist
                            }
                        }
                    }

                    if (clickedPart != null) {
                        onPartClicked(clickedPart)
                    } else if (editorModeActive) {
                        onCanvasTappedAtPercent(xPct, yPct)
                    }
                }
            }
    ) {
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        
        if (w <= 0 || h <= 0) return@Canvas

        // 1. Draw elegant background grid or sci-fi medical scan circles
        drawAnatomicalScannerGrid(w, h)

        // 2. Draw outer physical body outline
        drawBodyOutlineSilhouette(w, h, selectedView)

        // 3. Draw bones when Skeletal system is studied
        if (selectedSystem == BodySystem.SKELETAL) {
            drawSkeletonLayout(w, h, selectedView)
        }

        // 4. Draw organs when Organs system is studied
        if (selectedSystem == BodySystem.ORGANS || selectedView == BodyView.ORGANS) {
            drawOrgansLayout(w, h, heartPulse)
        }

        // 5. Render pinpoints / hotspots
        for (part in activeParts) {
            if (part.view == selectedView) {
                val pxX = part.xPercent * w
                val pxY = part.yPercent * h

                // Decide color based on study / quiz state
                val isSelected = part.id == selectedPart?.id
                val isQuizActiveItem = quizActivePart != null && part.id == quizActivePart.id
                
                val baseColor = when {
                    // Quiz correct: green highlight
                    quizCorrectPartsId.contains(part.id) -> Color(0xFF22C55E) 
                    // Quiz incorrect: red highlight
                    quizWrongPartsId.contains(part.id) -> Color(0xFFEF4444)
                    // Currently questioned under identification: pulsing amber
                    isQuizActiveItem -> Color(0xFFEAB308)
                    // Selected: intense cyan
                    isSelected -> Color(0xFF00E5FF)
                    // Default Custom node: pinkish purple
                    part.isCustom -> Color(0xFFE040FB)
                    // Standard color by system
                    part.system == BodySystem.SKELETAL -> Color(0xFFF1F5F9) // elegant slate white
                    part.system == BodySystem.ORGANS -> Color(0xFFFF8A80) // soft red
                    else -> Color(0xFF00E5FF) // neon cyan
                }

                // Pulsing accent halo
                val outerHaloRadius = if (isSelected || isQuizActiveItem) 28f * pulseRadiusScale else 18f * pulseRadiusScale
                val outerHaloAlpha = if (isSelected || isQuizActiveItem) pulseAlpha else pulseAlpha * 0.6f

                drawCircle(
                    color = baseColor.copy(alpha = outerHaloAlpha),
                    radius = outerHaloRadius,
                    center = Offset(pxX, pxY)
                )

                // Outer hard ring
                drawCircle(
                    color = baseColor,
                    radius = if (isSelected || isQuizActiveItem) 12f else 8f,
                    center = Offset(pxX, pxY),
                    style = Stroke(width = 3f)
                )

                // Inner solid core dot
                drawCircle(
                    color = if (isSelected || isQuizActiveItem) Color.White else baseColor,
                    radius = if (isSelected || isQuizActiveItem) 6f else 4f,
                    center = Offset(pxX, pxY)
                )

                // If selected, throw a subtle technical marker tag line in screen
                if (isSelected) {
                    drawTextCalloutLine(pxX, pxY, part, w, h)
                }
            }
        }
    }
}

/**
 * Draws a beautiful high-tech circular radar mesh in the background.
 */
private fun DrawScope.drawAnatomicalScannerGrid(w: Float, h: Float) {
    val center = Offset(w / 2f, h * 0.40f)
    
    // Grid Lines for background aesthetics
    val gridColor = Color(0xFF0F2027).copy(alpha = 0.3f)
    val lineCount = 8
    for (i in 1..lineCount) {
        val y = h * (i.toFloat() / (lineCount + 1))
        drawLine(
            color = Color(0xFF1F3C4D).copy(alpha = 0.08f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 2f
        )
    }

    // Concentric scan circles
    val scanColor = Color(0xFF00E5FF).copy(alpha = 0.04f)
    drawCircle(
        color = scanColor,
        radius = w * 0.35f,
        center = center,
        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
    )
    drawCircle(
        color = scanColor,
        radius = w * 0.18f,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

/**
 * Programmatic vector path rendering for the physical human silhouette body.
 * Keeps it beautifully streamlined and futuristic.
 */
private fun DrawScope.drawBodyOutlineSilhouette(w: Float, h: Float, view: BodyView) {
    val outlineColor = Color(0xFF2A2A2E)
    val fillColor = Color(0xFF131316).copy(alpha = 0.92f)
    
    val bodyPath = Path().apply {
        // Head (Oval path)
        val headY = h * 0.08f
        val headRadiusX = w * 0.09f
        val headRadiusY = h * 0.05f
        
        // Let's create a beautiful geometric representation of the body
        // 1. Neck neck and jaw
        moveTo(w * 0.5f, h * 0.03f) // top of head
        // Cubic curves for head left
        cubicTo(w * 0.43f, h * 0.03f, w * 0.41f, h * 0.06f, w * 0.41f, h * 0.08f)
        cubicTo(w * 0.41f, h * 0.11f, w * 0.45f, h * 0.13f, w * 0.46f, h * 0.14f) // chin left
        
        // Left Neck
        lineTo(w * 0.46f, h * 0.16f)
        
        // Left Shoulder
        cubicTo(w * 0.43f, h * 0.17f, w * 0.37f, h * 0.18f, w * 0.36f, h * 0.20f)
        
        // Left Arm Upper
        lineTo(w * 0.33f, h * 0.30f)
        // Left Elbow
        lineTo(w * 0.31f, h * 0.38f)
        // Left Forearm
        lineTo(w * 0.28f, h * 0.47f)
        
        // Left Hand
        cubicTo(w * 0.26f, h * 0.49f, w * 0.26f, h * 0.51f, w * 0.28f, h * 0.52f)
        lineTo(w * 0.30f, h * 0.50f)
        
        // Left Arm Inner
        lineTo(w * 0.34f, h * 0.41f)
        lineTo(w * 0.36f, h * 0.33f)
        
        // Left Torso / Axilla
        lineTo(w * 0.41f, h * 0.28f)
        
        // Left Waist
        lineTo(w * 0.41f, h * 0.44f)
        
        // Left Hip Pelvis
        cubicTo(w * 0.40f, h * 0.47f, w * 0.39f, h * 0.51f, w * 0.39f, h * 0.54f)
        
        // Left Thigh
        lineTo(w * 0.41f, h * 0.68f)
        
        // Left Knee
        lineTo(w * 0.41f, h * 0.74f)
        
        // Left Calf
        lineTo(w * 0.40f, h * 0.85f)
        
        // Left Ankle / Feet
        lineTo(w * 0.39f, h * 0.93f)
        cubicTo(w * 0.36f, h * 0.94f, w * 0.36f, h * 0.96f, w * 0.41f, h * 0.96f) // toe left
        lineTo(w * 0.45f, h * 0.95f) // arch
        lineTo(w * 0.47f, h * 0.93f) // ankle inner
        
        // Crotch
        lineTo(w * 0.50f, h * 0.55f)
        
        // Right Leg (mirroring left coordinates)
        lineTo(w * 0.53f, h * 0.93f) // right ankle inner
        lineTo(w * 0.55f, h * 0.95f) // right arch
        cubicTo(w * 0.64f, h * 0.96f, w * 0.64f, h * 0.94f, w * 0.61f, h * 0.93f) // toe right
        lineTo(w * 0.60f, h * 0.85f) // right calf
        lineTo(w * 0.59f, h * 0.74f) // right knee
        lineTo(w * 0.59f, h * 0.68f) // right thigh
        cubicTo(w * 0.61f, h * 0.51f, w * 0.60f, h * 0.47f, w * 0.61f, h * 0.54f) // right pelvic outer
        lineTo(w * 0.59f, h * 0.44f) // right waist
        lineTo(w * 0.59f, h * 0.28f) // right axilla
        
        // Right Arm Inner
        lineTo(w * 0.64f, h * 0.33f)
        lineTo(w * 0.66f, h * 0.41f)
        lineTo(w * 0.70f, h * 0.50f)
        
        // Right Hand
        cubicTo(w * 0.74f, h * 0.51f, w * 0.74f, h * 0.49f, w * 0.72f, h * 0.52f)
        lineTo(w * 0.72f, h * 0.47f) // right wrist
        lineTo(w * 0.69f, h * 0.38f) // right forearm
        lineTo(w * 0.67f, h * 0.30f) // right elbow
        lineTo(w * 0.64f, h * 0.20f) // right arm upper
        
        // Right Shoulder
        cubicTo(w * 0.63f, h * 0.18f, w * 0.57f, h * 0.17f, w * 0.54f, h * 0.16f)
        
        // Right Neck
        lineTo(w * 0.54f, h * 0.16f)
        lineTo(w * 0.54f, h * 0.14f)
        
        // Head right
        cubicTo(w * 0.55f, h * 0.13f, w * 0.59f, h * 0.11f, w * 0.59f, h * 0.08f)
        cubicTo(w * 0.59f, h * 0.06f, w * 0.57f, h * 0.03f, w * 0.50f, h * 0.03f)
        close()
    }

    // Draw the body filling with elegant midnight blue
    drawPath(
        path = bodyPath,
        color = fillColor
    )

    // Draw outline with high-tech cyan/steel stroke
    drawPath(
        path = bodyPath,
        color = outlineColor,
        style = Stroke(
            width = 4.5f,
            cap = StrokeCap.Round
        )
    )
    
    // Aesthetic accent: Neon lighting along the shoulders/neck
    drawPath(
        path = bodyPath,
        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
        style = Stroke(
            width = 1.5f
        )
    )
}

/**
 * Renders stylized medical bone outlines over the silhouette.
 */
private fun DrawScope.drawSkeletonLayout(w: Float, h: Float, view: BodyView) {
    val boneColor = Color(0xFFECEFF1).copy(alpha = 0.75f)
    val boneStroke = Stroke(width = 3.5f, cap = StrokeCap.Round)

    // 1. Skull lines
    drawCircle(
        color = boneColor.copy(alpha = 0.4f),
        radius = w * 0.06f,
        center = Offset(w * 0.5f, h * 0.08f),
        style = Stroke(width = 2f)
    )

    // 2. Clavicle lines
    drawLine(
        color = boneColor,
        start = Offset(w * 0.43f, h * 0.18f),
        end = Offset(w * 0.5f, h * 0.17f),
        strokeWidth = 3f
    )
    drawLine(
        color = boneColor,
        start = Offset(w * 0.57f, h * 0.18f),
        end = Offset(w * 0.5f, h * 0.17f),
        strokeWidth = 3f
    )

    // 3. Sternum central segment
    drawLine(
        color = boneColor,
        start = Offset(w * 0.5f, h * 0.18f),
        end = Offset(w * 0.5f, h * 0.32f),
        strokeWidth = 7f
    )

    // 4. Ribcage stylized rings
    for (i in 0..4) {
        val yOffset = h * (0.20f + i * 0.03f)
        val radiusX = w * (0.07f - i * 0.005f)
        drawOval(
            color = boneColor.copy(alpha = 0.25f),
            topLeft = Offset(w * 0.5f - radiusX, yOffset),
            size = Size(radiusX * 2f, h * 0.015f),
            style = Stroke(width = 2.5f)
        )
    }

    // 5. Spine vertebral column
    for (i in 0..12) {
        val y = h * (0.19f + i * 0.024f)
        drawRect(
            color = boneColor.copy(alpha = 0.6f),
            topLeft = Offset(w * 0.49f, y),
            size = Size(w * 0.02f, h * 0.012f)
        )
    }

    // 6. Pelvis ring
    drawOval(
        color = boneColor.copy(alpha = 0.4f),
        topLeft = Offset(w * 0.44f, h * 0.46f),
        size = Size(w * 0.12f, h * 0.06f),
        style = Stroke(width = 3.5f)
    )

    // 7. Right and Left Humerus
    drawLine(
        color = boneColor,
        start = Offset(w * 0.38f, h * 0.19f),
        end = Offset(w * 0.34f, h * 0.32f),
        strokeWidth = 4.5f
    )
    drawLine(
        color = boneColor,
        start = Offset(w * 0.62f, h * 0.19f),
        end = Offset(w * 0.66f, h * 0.32f),
        strokeWidth = 4.5f
    )

    // 8. Right and Left Ulna/Radius bones
    drawLine(
        color = boneColor,
        start = Offset(w * 0.34f, h * 0.32f),
        end = Offset(w * 0.30f, h * 0.47f),
        strokeWidth = 3f
    )
    drawLine(
        color = boneColor,
        start = Offset(w * 0.66f, h * 0.32f),
        end = Offset(w * 0.70f, h * 0.47f),
        strokeWidth = 3f
    )

    // 9. Hip to knee femurs
    drawLine(
        color = boneColor,
        start = Offset(w * 0.44f, h * 0.50f),
        end = Offset(w * 0.43f, h * 0.72f),
        strokeWidth = 5.5f
    )
    drawLine(
        color = boneColor,
        start = Offset(w * 0.56f, h * 0.50f),
        end = Offset(w * 0.57f, h * 0.72f),
        strokeWidth = 5.5f
    )

    // 10. Tibia/Fibula
    drawLine(
        color = boneColor,
        start = Offset(w * 0.43f, h * 0.72f),
        end = Offset(w * 0.41f, h * 0.92f),
        strokeWidth = 4f
    )
    drawLine(
        color = boneColor.copy(alpha = 0.5f),
        start = Offset(w * 0.44f, h * 0.72f),
        end = Offset(w * 0.43f, h * 0.91f),
        strokeWidth = 2f
    )
    
    drawLine(
        color = boneColor,
        start = Offset(w * 0.57f, h * 0.72f),
        end = Offset(w * 0.59f, h * 0.92f),
        strokeWidth = 4f
    )
    drawLine(
        color = boneColor.copy(alpha = 0.5f),
        start = Offset(w * 0.56f, h * 0.72f),
        end = Offset(w * 0.57f, h * 0.91f),
        strokeWidth = 2f
    )
}

/**
 * Draws stylized organ graphics over the anatomical silhouette.
 */
private fun DrawScope.drawOrgansLayout(w: Float, h: Float, heartPulse: Float) {
    // 1. Brain inside skull
    drawCircle(
        color = Color(0xFFFFB74D).copy(alpha = 0.35f),
        radius = w * 0.05f,
        center = Offset(w * 0.5f, h * 0.07f)
    )
    
    // 2. Trachea / Larynx
    drawRect(
        color = Color(0xFF4DD0E1).copy(alpha = 0.4f),
        topLeft = Offset(w * 0.485f, h * 0.13f),
        size = Size(w * 0.03f, h * 0.04f)
    )

    // 3. Lungs flanking the mediastinum
    val lungPathL = Path().apply {
        moveTo(w * 0.48f, h * 0.19f)
        quadraticTo(w * 0.39f, h * 0.20f, w * 0.39f, h * 0.28f)
        quadraticTo(w * 0.41f, h * 0.31f, w * 0.48f, h * 0.29f)
        close()
    }
    val lungPathR = Path().apply {
        moveTo(w * 0.52f, h * 0.19f)
        quadraticTo(w * 0.61f, h * 0.20f, w * 0.61f, h * 0.28f)
        quadraticTo(w * 0.59f, h * 0.31f, w * 0.52f, h * 0.29f)
        close()
    }
    
    drawPath(lungPathL, Color(0xFF90CAF9).copy(alpha = 0.35f))
    drawPath(lungPathR, Color(0xFF90CAF9).copy(alpha = 0.35f))

    // 4. Heart: Pulsing red organ
    drawCircle(
        color = Color(0xFFEF5350).copy(alpha = 0.6f),
        radius = w * 0.038f * heartPulse,
        center = Offset(w * 0.51f, h * 0.26f)
    )
    
    // 5. Liver under right lung
    val liverPath = Path().apply {
        moveTo(w * 0.41f, h * 0.33f)
        lineTo(w * 0.49f, h * 0.33f)
        lineTo(w * 0.46f, h * 0.38f)
        close()
    }
    drawPath(liverPath, Color(0xFFFFB74D).copy(alpha = 0.45f))

    // 6. Stomach opposite liver
    val stomachPath = Path().apply {
        moveTo(w * 0.50f, h * 0.33f)
        quadraticTo(w * 0.59f, h * 0.34f, w * 0.55f, h * 0.39f)
        quadraticTo(w * 0.49f, h * 0.38f, w * 0.50f, h * 0.33f)
        close()
    }
    drawPath(stomachPath, Color(0xFFBA68C8).copy(alpha = 0.45f))

    // 7. Intestine group
    drawOval(
        color = Color(0xFFFFD54F).copy(alpha = 0.35f),
        topLeft = Offset(w * 0.45f, h * 0.43f),
        size = Size(w * 0.10f, h * 0.07f)
    )
    
    // 8. Bladder
    drawCircle(
        color = Color(0xFFFF8A65).copy(alpha = 0.5f),
        radius = w * 0.022f,
        center = Offset(w * 0.50f, h * 0.53f)
    )
}

/**
 * Technical aesthetic tag lines pointing to the coordinates of the selected pin.
 */
private fun DrawScope.drawTextCalloutLine(pxX: Float, pxY: Float, part: BodyPart, w: Float, h: Float) {
    val lineCol = Color(0xFF00E5FF).copy(alpha = 0.5f)
    val textOffset = if (pxX < w * 0.5f) -w * 0.15f else w * 0.15f
    
    // Joint pinpoint line
    drawLine(
        color = lineCol,
        start = Offset(pxX, pxY),
        end = Offset(pxX + textOffset * 0.4f, pxY - h * 0.03f),
        strokeWidth = 2.5f
    )
    // Horizontal secondary slider line
    drawLine(
        color = lineCol,
        start = Offset(pxX + textOffset * 0.4f, pxY - h * 0.03f),
        end = Offset(pxX + textOffset, pxY - h * 0.03f),
        strokeWidth = 2.5f
    )
}
