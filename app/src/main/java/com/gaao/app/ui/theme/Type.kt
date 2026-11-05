package com.gaao.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gaao.app.R

// Main Lyrics - Hindi
val HindiFamily = FontFamily(
    Font(R.font.hindi, FontWeight.Normal)
)

// Main Lyrics - English
val EnglishFamily = FontFamily(
    Font(R.font.english, FontWeight.Bold)
)

val EnglishFamilyBold = FontFamily(
    Font(R.font.english, FontWeight.Bold)
)

// Secondary info & header text
val EnglishSecondary = FontFamily(
    Font(R.font.english_secondary, FontWeight.Normal)
)

fun isHindiOrDevanagari(text: String): Boolean {
    return text.any { it in '\u0900'..'\u097F' }
}

fun getMainLyricsFont(text: String): FontFamily {
    return if (isHindiOrDevanagari(text)) HindiFamily else EnglishFamily
}

fun getSecondaryFont(): FontFamily = EnglishSecondary

// Backward compatibility aliases
val TimesNewRomanFamily: FontFamily = EnglishSecondary
val BasicCommerceFamily: FontFamily = EnglishFamily
val AmsBhagyshriFamily: FontFamily = HindiFamily
val BasicCommerceItalicFamily: FontFamily = EnglishFamily

// Backward compatibility helper
fun getPosterFontFamily(text: String): FontFamily = getMainLyricsFont(text)
fun getHeroFontFamily(text: String): FontFamily = getMainLyricsFont(text)
fun getAccentFontFamily(text: String): FontFamily = getMainLyricsFont(text)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = EnglishSecondary,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
