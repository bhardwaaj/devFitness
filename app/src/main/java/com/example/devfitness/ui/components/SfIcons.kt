package com.example.devfitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun EnvelopeIcon(tint: Color, modifier: Modifier = Modifier) {
    SfCanvas(modifier, decorative = true) { unit ->
        val stroke = stroke(unit)
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 3f * unit,
                    top = 6.4f * unit,
                    right = 21f * unit,
                    bottom = 17.6f * unit,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * unit, 2f * unit),
                ),
            )
        }
        drawPath(body, tint, style = stroke)
        val flap = Path().apply {
            moveTo(4.1f * unit, 7.5f * unit)
            lineTo(12f * unit, 12.5f * unit)
            lineTo(19.9f * unit, 7.5f * unit)
        }
        drawPath(flap, tint, style = stroke)
    }
}

@Composable
fun LockIcon(tint: Color, modifier: Modifier = Modifier) {
    SfCanvas(modifier, decorative = true) { unit ->
        val stroke = stroke(unit)
        val body = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 5.4f * unit,
                    top = 10.6f * unit,
                    right = 18.6f * unit,
                    bottom = 20.2f * unit,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * unit, 2f * unit),
                ),
            )
        }
        drawPath(body, tint, style = stroke)
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(7.3f * unit, 4.1f * unit),
            size = Size(9.4f * unit, 9.2f * unit),
            style = stroke,
        )
    }
}

@Composable
fun PersonIcon(tint: Color, modifier: Modifier = Modifier) {
    SfCanvas(modifier, decorative = true) { unit ->
        val stroke = stroke(unit)
        drawCircle(
            color = tint,
            radius = 3.05f * unit,
            center = Offset(12f * unit, 7.8f * unit),
            style = stroke,
        )
        val shoulders = Path().apply {
            moveTo(4.8f * unit, 19.6f * unit)
            cubicTo(
                5.4f * unit, 15.1f * unit,
                8.2f * unit, 13.2f * unit,
                12f * unit, 13.2f * unit,
            )
            cubicTo(
                15.8f * unit, 13.2f * unit,
                18.6f * unit, 15.1f * unit,
                19.2f * unit, 19.6f * unit,
            )
        }
        drawPath(shoulders, tint, style = stroke)
    }
}

@Composable
fun EyeIcon(tint: Color, visible: Boolean, modifier: Modifier = Modifier) {
    SfCanvas(modifier, decorative = true) { unit ->
        val stroke = stroke(unit)
        val eye = Path().apply {
            moveTo(2.4f * unit, 12f * unit)
            cubicTo(5.6f * unit, 7.1f * unit, 8.6f * unit, 5.5f * unit, 12f * unit, 5.5f * unit)
            cubicTo(15.4f * unit, 5.5f * unit, 18.4f * unit, 7.1f * unit, 21.6f * unit, 12f * unit)
            cubicTo(18.4f * unit, 16.9f * unit, 15.4f * unit, 18.5f * unit, 12f * unit, 18.5f * unit)
            cubicTo(8.6f * unit, 18.5f * unit, 5.6f * unit, 16.9f * unit, 2.4f * unit, 12f * unit)
            close()
        }
        drawPath(eye, tint, style = stroke)
        drawCircle(
            color = tint,
            radius = 1.9f * unit,
            center = Offset(12f * unit, 12f * unit),
            style = stroke,
        )
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(5f * unit, 18.4f * unit),
                end = Offset(19f * unit, 5.6f * unit),
                strokeWidth = 1.8f * unit,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun ChevronLeftIcon(tint: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    SfCanvas(modifier, contentDescription = contentDescription) { unit ->
        val path = Path().apply {
            moveTo(14.6f * unit, 5.4f * unit)
            lineTo(8.2f * unit, 12f * unit)
            lineTo(14.6f * unit, 18.6f * unit)
        }
        drawPath(path, tint, style = stroke(unit, width = 2.05f))
    }
}

@Composable
private fun SfCanvas(
    modifier: Modifier,
    contentDescription: String? = null,
    decorative: Boolean = false,
    draw: DrawScope.(unit: Float) -> Unit,
) {
    val semanticsModifier = when {
        contentDescription != null -> Modifier.semantics { this.contentDescription = contentDescription }
        decorative -> Modifier.clearAndSetSemantics { }
        else -> Modifier
    }
    Canvas(modifier.then(semanticsModifier)) {
        draw(size.minDimension / 24f)
    }
}

private fun DrawScope.stroke(unit: Float, width: Float = 1.75f) = Stroke(
    width = width * unit,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)
