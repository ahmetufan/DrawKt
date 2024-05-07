package com.canvas

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.canvas.drawing.utils.ColorSelectionDialog
import com.canvas.drawing.utils.ColorWheel
import com.canvas.drawing.utils.DrawMode
import com.canvas.drawing.utils.ExposedSelectionMenu
import com.canvas.drawing.R
import com.canvas.drawing.gesture.MotionEvent
import com.canvas.drawing.gesture.dragMotionEvent
import com.canvas.drawing.ui.theme.Blue400
import com.canvas.drawing.ui.theme.backgroundColor

@Composable
fun DrawingScreen() {


    val context = LocalContext.current

    val paths = remember { mutableStateListOf<Pair<Path, PathProperties>>() }

    val pathsUndone = remember { mutableStateListOf<Pair<Path, PathProperties>>() }

    var motionEvent by remember { mutableStateOf(MotionEvent.Idle) }

    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }

    var previousPosition by remember { mutableStateOf(Offset.Unspecified) }

    var drawMode by remember { mutableStateOf(DrawMode.Draw) }

    var currentPath by remember { mutableStateOf(Path()) }

    var currentPathProperty by remember { mutableStateOf(PathProperties()) }

    val canvasText = remember { StringBuilder() }
    val paint = remember {
        Paint().apply {
            textSize = 40f
            color = Color.Black.toArgb()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        val drawModifier = Modifier
            .padding(8.dp)
            .shadow(1.dp)
            .fillMaxWidth()
            .weight(1f)
            .background(Color.White)
//            .background(getRandomColor())
            .dragMotionEvent(
                onDragStart = { pointerInputChange ->
                    motionEvent = MotionEvent.Down
                    currentPosition = pointerInputChange.position
                    pointerInputChange.consume()

                },
                onDrag = { pointerInputChange ->
                    motionEvent = MotionEvent.Move
                    currentPosition = pointerInputChange.position

                    if (drawMode == DrawMode.Touch) {
                        val change = pointerInputChange.positionChange()
                        println("DRAG: $change")
                        paths.forEach { entry ->
                            val path: Path = entry.first
                            path.translate(change)
                        }
                        currentPath.translate(change)
                    }
                    pointerInputChange.consume()

                },
                onDragEnd = { pointerInputChange ->
                    motionEvent = MotionEvent.Up
                    pointerInputChange.consume()
                }
            )

        Canvas(modifier = drawModifier) {

            when (motionEvent) {

                MotionEvent.Down -> {
                    if (drawMode != DrawMode.Touch) {
                        currentPath.moveTo(currentPosition.x, currentPosition.y)
                    }

                    previousPosition = currentPosition

                }

                MotionEvent.Move -> {

                    if (drawMode != DrawMode.Touch) {
                        currentPath.quadraticBezierTo(
                            previousPosition.x,
                            previousPosition.y,
                            (previousPosition.x + currentPosition.x) / 2,
                            (previousPosition.y + currentPosition.y) / 2

                        )
                    }

                    previousPosition = currentPosition
                }

                MotionEvent.Up -> {
                    if (drawMode != DrawMode.Touch) {
                        currentPath.lineTo(currentPosition.x, currentPosition.y)

                        paths.add(Pair(currentPath, currentPathProperty))

                        currentPath = Path()

                        currentPathProperty = PathProperties(
                            strokeWidth = currentPathProperty.strokeWidth,
                            color = currentPathProperty.color,
                            strokeCap = currentPathProperty.strokeCap,
                            strokeJoin = currentPathProperty.strokeJoin,
                            eraseMode = currentPathProperty.eraseMode
                        )
                    }

                    pathsUndone.clear()

                    currentPosition = Offset.Unspecified
                    previousPosition = currentPosition
                    motionEvent = MotionEvent.Idle
                }

                else -> Unit
            }

            with(drawContext.canvas.nativeCanvas) {

                val checkPoint = saveLayer(null, null)

                paths.forEach {

                    val path = it.first
                    val property = it.second

                    if (!property.eraseMode) {
                        drawPath(
                            color = property.color,
                            path = path,
                            style = Stroke(
                                width = property.strokeWidth,
                                cap = property.strokeCap,
                                join = property.strokeJoin
                            )
                        )
                    } else {

                        // Source
                        drawPath(
                            color = Color.Transparent,
                            path = path,
                            style = Stroke(
                                width = property.strokeWidth,
                                cap = property.strokeCap,
                                join = property.strokeJoin
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                }

                if (motionEvent != MotionEvent.Idle) {

                    if (!currentPathProperty.eraseMode) {
                        drawPath(
                            color = currentPathProperty.color,
                            path = currentPath,
                            style = Stroke(
                                width = currentPathProperty.strokeWidth,
                                cap = currentPathProperty.strokeCap,
                                join = currentPathProperty.strokeJoin
                            )
                        )
                    } else {
                        drawPath(
                            color = Color.Transparent,
                            path = currentPath,
                            style = Stroke(
                                width = currentPathProperty.strokeWidth,
                                cap = currentPathProperty.strokeCap,
                                join = currentPathProperty.strokeJoin
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
                restoreToCount(checkPoint)
            }
        }

        DrawingPropertiesMenu(
            modifier = Modifier
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                .shadow(1.dp, RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .background(Color.White)
                .padding(4.dp),
            pathProperties = currentPathProperty,
            drawMode = drawMode,
            onUndo = {
                if (paths.isNotEmpty()) {

                    val lastItem = paths.last()
                    val lastPath = lastItem.first
                    val lastPathProperty = lastItem.second
                    paths.remove(lastItem)

                    pathsUndone.add(Pair(lastPath, lastPathProperty))

                }
            },
            onRedo = {
                if (pathsUndone.isNotEmpty()) {

                    val lastPath = pathsUndone.last().first
                    val lastPathProperty = pathsUndone.last().second
                    pathsUndone.removeLast()
                    paths.add(Pair(lastPath, lastPathProperty))
                }
            },
            onPathPropertiesChange = {
                motionEvent = MotionEvent.Idle
            },
            onDrawModeChanged = {
                motionEvent = MotionEvent.Idle
                drawMode = it
                currentPathProperty.eraseMode = (drawMode == DrawMode.Erase)

            }
        )
    }
}

@Composable
private fun DrawingPropertiesMenu(
    modifier: Modifier = Modifier,
    pathProperties: PathProperties,
    drawMode: DrawMode,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPathPropertiesChange: (PathProperties) -> Unit,
    onDrawModeChanged: (DrawMode) -> Unit
) {

    val properties by rememberUpdatedState(newValue = pathProperties)

    var showColorDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var currentDrawMode = drawMode

    Row(
        modifier = modifier
//            .background(getRandomColor())
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = {
                currentDrawMode = if (currentDrawMode == DrawMode.Touch) {
                    DrawMode.Draw
                } else {
                    DrawMode.Touch
                }
                onDrawModeChanged(currentDrawMode)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.touch_icon),
                contentDescription = null,
                tint = if (currentDrawMode == DrawMode.Touch) Color.Black else Color.LightGray
            )
        }
        IconButton(
            onClick = {
                currentDrawMode = if (currentDrawMode == DrawMode.Erase) {
                    DrawMode.Draw
                } else {
                    DrawMode.Erase
                }
                onDrawModeChanged(currentDrawMode)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_eraser_black_24dp),
                contentDescription = null,
                tint = if (currentDrawMode == DrawMode.Erase) Color.Black else Color.LightGray
            )
        }


        IconButton(onClick = { showColorDialog = !showColorDialog }) {
            ColorWheel(modifier = Modifier.size(24.dp))
        }

        IconButton(onClick = { showPropertiesDialog = !showPropertiesDialog }) {
            Icon(Icons.Filled.Create, contentDescription = null, tint = Color.LightGray)
        }

        IconButton(onClick = {
            onUndo()
        }) {
            Icon(
                painter = painterResource(id = R.drawable.back_icon),
                contentDescription = null,
                tint = Color.LightGray
            )
        }

        IconButton(onClick = {
            onRedo()
        }) {
            Icon(
                painter = painterResource(id = R.drawable.next_icon),
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }

    if (showColorDialog) {
        ColorSelectionDialog(
            properties.color,
            onDismiss = { showColorDialog = !showColorDialog },
            onNegativeClick = { showColorDialog = !showColorDialog },
            onPositiveClick = { color: Color ->
                showColorDialog = !showColorDialog
                properties.color = color
            }
        )
    }

    if (showPropertiesDialog) {
        PropertiesMenuDialog(properties) {
            showPropertiesDialog = !showPropertiesDialog
        }
    }
}

@Composable
internal fun PropertiesMenuDialog(pathOption: PathProperties, onDismiss: () -> Unit) {

    var strokeWidth by remember { mutableStateOf(pathOption.strokeWidth) }
    var strokeCap by remember { mutableStateOf(pathOption.strokeCap) }
    var strokeJoin by remember { mutableStateOf(pathOption.strokeJoin) }

    Dialog(onDismissRequest = onDismiss) {

        Card(
            elevation = 2.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = "Properties",
                    color = Blue400,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp)
                )

                Canvas(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    val path = Path()
                    path.moveTo(0f, size.height / 2)
                    path.lineTo(size.width, size.height / 2)

                    drawPath(
                        color = pathOption.color,
                        path = path,
                        style = Stroke(
                            width = strokeWidth,
                            cap = strokeCap,
                            join = strokeJoin
                        )
                    )
                }

                Text(
                    text = "Stroke Width ${strokeWidth.toInt()}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Slider(
                    value = strokeWidth,
                    onValueChange = {
                        strokeWidth = it
                        pathOption.strokeWidth = strokeWidth
                    },
                    valueRange = 1f..100f,
                    onValueChangeFinished = {}
                )


                ExposedSelectionMenu(title = "Stroke Cap",
                    index = when (strokeCap) {
                        StrokeCap.Butt -> 0
                        StrokeCap.Round -> 1
                        else -> 2
                    },
                    options = listOf("Butt", "Round", "Square"),
                    onSelected = {
                        println("STOKE CAP $it")
                        strokeCap = when (it) {
                            0 -> StrokeCap.Butt
                            1 -> StrokeCap.Round
                            else -> StrokeCap.Square
                        }

                        pathOption.strokeCap = strokeCap

                    }
                )

                ExposedSelectionMenu(title = "Stroke Join",
                    index = when (strokeJoin) {
                        StrokeJoin.Miter -> 0
                        StrokeJoin.Round -> 1
                        else -> 2
                    },
                    options = listOf("Miter", "Round", "Bevel"),
                    onSelected = {
                        println("STOKE JOIN $it")

                        strokeJoin = when (it) {
                            0 -> StrokeJoin.Miter
                            1 -> StrokeJoin.Round
                            else -> StrokeJoin.Bevel
                        }

                        pathOption.strokeJoin = strokeJoin
                    }
                )
            }
        }
    }
}

class PathProperties(
    var strokeWidth: Float = 10f,
    var color: Color = Color.Black,
    var alpha: Float = 1f,
    var strokeCap: StrokeCap = StrokeCap.Round,
    var strokeJoin: StrokeJoin = StrokeJoin.Round,
    var eraseMode: Boolean = false
) {

    fun copy(
        strokeWidth: Float = this.strokeWidth,
        color: Color = this.color,
        alpha: Float = this.alpha,
        strokeCap: StrokeCap = this.strokeCap,
        strokeJoin: StrokeJoin = this.strokeJoin,
        eraseMode: Boolean = this.eraseMode
    ) = PathProperties(
        strokeWidth, color, alpha, strokeCap, strokeJoin, eraseMode
    )

    fun copyFrom(properties: PathProperties) {
        this.strokeWidth = properties.strokeWidth
        this.color = properties.color
        this.strokeCap = properties.strokeCap
        this.strokeJoin = properties.strokeJoin
        this.eraseMode = properties.eraseMode
    }
}

