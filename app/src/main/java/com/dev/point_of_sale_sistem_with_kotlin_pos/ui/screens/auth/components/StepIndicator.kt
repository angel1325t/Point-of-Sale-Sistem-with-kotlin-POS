// ui/screens/auth/components/StepIndicator.kt
package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepTitles: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..totalSteps) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                step < currentStep -> MaterialTheme.colorScheme.primary
                                step == currentStep -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step < currentStep) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = step.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (step == currentStep)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (step < totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 16.dp)
                            .background(
                                if (step < currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepTitles.forEachIndexed { index, title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index + 1 == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index + 1 == currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}