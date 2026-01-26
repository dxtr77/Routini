package com.dxtr.routini.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.ui.composables.GlassCard
import com.dxtr.routini.ui.composables.GradientButton
import com.dxtr.routini.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Skip button
        androidx.compose.material3.TextButton(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                viewModel.setFirstLaunchComplete()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(
                "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn() + 
                            androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { it / 2 }
                            )
                ) {
                    OnboardingPage(page = page)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Indicators with animation
            Row(
                modifier = Modifier.height(50.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 10.dp,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                        ),
                        label = "indicator_width"
                    )
                    val color by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        label = "indicator_color"
                    )
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(width = width, height = 10.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GradientButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.setFirstLaunchComplete()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                },
                text = if (pagerState.currentPage == 2) "Get Started" else "Next",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    val title = when (page) {
        0 -> "Welcome to Routini"
        1 -> "Build Better Habits"
        2 -> "Track Your Progress"
        else -> ""
    }
    
    val description = when (page) {
        0 -> "Your personal companion for building lasting routines and achieving your daily goals."
        1 -> "Create custom routines, set tasks, and organize your day with ease."
        2 -> "Visualize your achievements and stay motivated with insightful statistics."
        else -> ""
    }
    
    val icon = when (page) {
        0 -> com.dxtr.routini.ui.theme.AppIcons.CheckCircle
        1 -> com.dxtr.routini.ui.theme.AppIcons.List
        2 -> com.dxtr.routini.ui.theme.AppIcons.TaskAlt
        else -> com.dxtr.routini.ui.theme.AppIcons.CheckCircle
    }

    GlassCard {
         Column(
             horizontalAlignment = Alignment.CenterHorizontally,
             verticalArrangement = Arrangement.Center,
             modifier = Modifier.padding(32.dp)
         ) {
             // Icon with gradient background
             Box(
                 modifier = Modifier
                     .size(80.dp)
                     .clip(CircleShape)
                     .background(
                         brush = Brush.linearGradient(
                             colors = listOf(
                                 MaterialTheme.colorScheme.primary,
                                 MaterialTheme.colorScheme.tertiary
                             )
                         )
                     ),
                 contentAlignment = Alignment.Center
             ) {
                 androidx.compose.material3.Icon(
                     painter = androidx.compose.ui.res.painterResource(id = icon),
                     contentDescription = null,
                     tint = MaterialTheme.colorScheme.onPrimary,
                     modifier = Modifier.size(40.dp)
                 )
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             Text(
                 text = title,
                 style = MaterialTheme.typography.headlineMedium,
                 fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onSurface,
                 textAlign = TextAlign.Center
             )
             Spacer(modifier = Modifier.height(16.dp))
             Text(
                 text = description,
                 style = MaterialTheme.typography.bodyLarge,
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                 textAlign = TextAlign.Center,
                 lineHeight = 24.sp
             )
         }
    }
}
