package com.motive.app.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable fun MotiveTheme(content:@Composable ()->Unit){
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFD6B45A),onPrimary=Color(0xFF171717),background=Color(0xFF0B0D10),surface=Color(0xFF15191F),onSurface=Color(0xFFF4F4F4),surfaceVariant=Color(0xFF242A33)),content=content)
}