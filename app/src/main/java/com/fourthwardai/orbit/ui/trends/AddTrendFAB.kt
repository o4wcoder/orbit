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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.fourthwardai.orbit.extensions.HorizontalSpacer
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme

/*
 * Floating action button to add a new tred. This FAB morphs into a container
 * where the user will enter there trend research query.
 *
 * Animation sequencing (Material container transform style):
 *
 * 1) Container morphs first (size, shape, color) — establishes structure
 *    Duration: ~320ms, FastOutSlowInEasing
 *
 * 2) FAB icon fades out early (~120ms) so the action identity disappears quickly
 *
 * 3) Expanded content fades in after ~180ms (~60% of container expansion),
 *    preventing content from appearing inside a still-circular FAB
 *
 * Result: FAB visually becomes a surface, then reveals content.
 */

@Composable
fun AddTrendFAB(
    expanded: Boolean,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    fabSize: Dp = 56.dp,
    expandedHeight: Dp = 260.dp,
    collapsedCornerRadius: Dp = 16.dp,
    expandedCornerRadius: Dp = 20.dp,
    edgePadding: Dp = 16.dp,
    scrimEnabled: Boolean = true,
    onExpandedChanged: (Boolean) -> Unit,
    onConfirm: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = expanded) {
        onExpandedChanged(false)
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
                        onExpandedChanged(false)
                    },
            )
        }

        val bottomOffset = innerPadding.calculateBottomPadding() + 16.dp
        // The morphing Surface anchored bottom-right
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(corner),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = edgePadding, end = edgePadding, bottom = bottomOffset)
                .width(width)
                .height(height)
                .shadow(10.dp, RoundedCornerShape(corner), clip = false)
                .clip(RoundedCornerShape(corner))
                .clickable(
                    enabled = !expanded,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onExpandedChanged(true)
                },
        ) {
            Crossfade(
                targetState = expanded,
                label = "contentCrossfade",
            ) { isExpanded ->
                if (!isExpanded) {
                    CollapsedFAB()
                } else {
                    ExpandedFAB(
                        query = query,
                        expanded = expanded,
                        onConfirm = onConfirm,
                        onQueryChange = { query = it },
                        onExpandedChanged = onExpandedChanged,
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsedFAB(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
        )
    }
}

@Composable
fun ExpandedFAB(
    query: String,
    expanded: Boolean,
    onConfirm: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 140,
            delayMillis = if (expanded) 180 else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "contentAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .graphicsLayer { alpha = contentAlpha },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.trends_query_label),
            style = MaterialTheme.typography.titleMedium,
        )

        VerticalSpacer(8.dp)

        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = false,
            minLines = 4,
            placeholder = { Text(text = stringResource(R.string.trends_query_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onConfirm(query)
                    onExpandedChanged(false)
                },
            ),
            shape = RoundedCornerShape(16.dp),
        )

        VerticalSpacer(12.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    onExpandedChanged(false)
                },
            ) { Text(stringResource(R.string.trends_cancel)) }

            HorizontalSpacer(8.dp)

            Button(
                onClick = {
                    onConfirm(query)
                    onExpandedChanged(false)
                },
                enabled = query.isNotBlank(),
            ) { Text(stringResource(R.string.trends_research)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrends() {
    OrbitTheme {
        AddTrendFAB(
            expanded = false,
            innerPadding = PaddingValues(0.dp),
            onExpandedChanged = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrendsExpanded() {
    OrbitTheme {
        AddTrendFAB(
            expanded = true,
            innerPadding = PaddingValues(0.dp),
            onExpandedChanged = {},
            onConfirm = {},
        )
    }
}
