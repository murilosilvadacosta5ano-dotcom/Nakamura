package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CherryBombFontFamily
import kotlinx.coroutines.delay

@Composable
fun WelcomeSplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Unique entry animations: Fade-in, scales, and rotation
    var visible by remember { mutableStateOf(false) }
    
    // Pulse animation for the glowing halo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )
    val rotationDegree by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotating_glow"
    )

    // Trigger exit delayed
    LaunchedEffect(Unit) {
        visible = true
        delay(3200) // Beautiful cinematic delay
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Midnight Dark Slate
                        Color(0xFF1E1B4B), // Indigo Depth
                        Color(0xFF020617)  // Obsidian bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glowing floating spots (The cosmic particles)
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-120).dp)
                .blur(80.dp)
                .alpha(0.15f)
                .background(Color(0xFFEC4899), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 100.dp, y = 140.dp)
                .blur(100.dp)
                .alpha(0.12f)
                .background(Color(0xFF3B82F6), CircleShape)
        )

        // Intro scaling-up animation block
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1200)) + 
                    scaleIn(animationSpec = tween(1200, easing = EaseOutBack)) +
                    slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(1200)),
            exit = fadeOut(animationSpec = tween(600)) + scaleOut(animationSpec = tween(600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Glow & Halo wrapper for logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Unique Glowing Ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .graphicsLayer(rotationZ = rotationDegree)
                            .alpha(haloAlpha)
                            .blur(4.dp)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6), // Ocean Blue
                                        Color(0xFFEC4899), // Pink
                                        Color(0xFF8B5CF6), // Purple
                                        Color(0xFF3B82F6)  // Wrap
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Central Image Logo
                    Box(
                        modifier = Modifier
                            .size(115.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nakamura_logo),
                            contentDescription = "Nakamura AI Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Beautiful title using Google Fonts "Cherry Bomb One"
                Text(
                    text = "Nakamura AI",
                    fontFamily = CherryBombFontFamily,
                    fontSize = 44.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Format: (título) (texto) requested
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "PRÓXIMA GERAÇÃO: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFA5B4FC) // Radiant Indigo Pastel
                    )
                    Text(
                        text = "Inteligência Artificial Pura",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Elegant Minimal Loader Indicator
                LinearProgressIndicator(
                    trackColor = Color(0xFF1E293B),
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .width(140.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}
