package com.fourthwardai.orbit.ui.trends

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun Trends(
    modifier: Modifier = Modifier,
    fabSize: Dp = 56.dp,
    expandedHeight: Dp = 240.dp,
    collapsedCornerRadius: Dp = 16.dp,
    expandedCornerRadius: Dp = 20.dp,
    edgePadding: Dp = 16.dp,
    scrimEnabled: Boolean = true,
    isExpandedDefault: Boolean = false,
    onConfirm: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(isExpandedDefault) }
    var query by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = expanded) {
        expanded = false
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxW = maxWidth
        val expandedWidth = (maxW - edgePadding * 2).coerceAtLeast(fabSize)

        val animSpec = tween<Dp>(durationMillis = 320, easing = FastOutSlowInEasing)
        val width by animateDpAsState(
            targetValue = if (expanded) expandedWidth else fabSize,
            animationSpec = animSpec,
            label = "panelWidth",
        )
        val height by animateDpAsState(
            targetValue = if (expanded) expandedHeight else fabSize,
            animationSpec = animSpec,
            label = "panelHeight",
        )
        val corner by animateDpAsState(
            targetValue = if (expanded) expandedCornerRadius else collapsedCornerRadius,
            animationSpec = animSpec,
            label = "panelCorner",
        )

        val scrimAlpha by animateFloatAsState(
            targetValue = if (expanded && scrimEnabled) 0.45f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "scrimAlpha",
        )

        val containerColor by animateColorAsState(
            targetValue = if (expanded) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            label = "containerColor",
        )

        val contentColor by animateColorAsState(
            targetValue = if (expanded) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            label = "contentColor",
        )

        val contentAlpha by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = 140,
                delayMillis = if (expanded) 180 else 0,
                easing = FastOutSlowInEasing,
            ),
            label = "contentAlpha",
        )

        // Scrim + outside tap to dismiss
        if (scrimAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        expanded = false
                    },
            )
        }

        // The morphing Surface anchored bottom-right
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(corner),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(edgePadding)
                .width(width)
                .height(height)
                .shadow(10.dp, RoundedCornerShape(corner), clip = false)
                .clip(RoundedCornerShape(corner))
                .clickable(
                    enabled = !expanded,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    expanded = true
                },
        ) {
            Crossfade(
                targetState = expanded,
                label = "contentCrossfade",
            ) { isExpanded ->
                if (!isExpanded) {
                    // Collapsed: Simple floating action button
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.trends_query_label),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth(),
                            singleLine = false,
                            minLines = 3,
                            placeholder = { Text(text = stringResource(R.string.trends_query_hint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onConfirm(query)
                                    expanded = false
                                },
                            ),
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    expanded = false
                                },
                            ) { Text(stringResource(R.string.trends_cancel)) }

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onConfirm(query)
                                    expanded = false
                                },
                                enabled = query.isNotBlank(),
                            ) { Text(stringResource(R.string.trends_research)) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrends() {
    OrbitTheme {
        Trends(onConfirm = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrendsExpanded() {
    OrbitTheme {
        Trends(isExpandedDefault = true, onConfirm = {})
    }
}
