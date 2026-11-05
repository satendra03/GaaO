# Fonts Tweaks

## 1. 🎛️ Lyrics Font Sizes & Padding

Open `PosterLyricScene.kt` around line 40:

```kotlin
// In: app/src/main/java/com/gaao/app/presentation/components/PosterLyricScene.kt
object LyricsSizeConfig {
    // English font sizes
    val ENGLISH_HERO_SIZE = 68.sp          // Size of the BIG focal word (e.g., "KNOW,", "WANNA", "DID")
    val ENGLISH_HERO_LINE_HEIGHT = 72.sp
    val ENGLISH_NORMAL_SIZE = 34.sp        // Size of supporting words (e.g., "I SHOULD", "AND I DON'T")
    val ENGLISH_NORMAL_LINE_HEIGHT = 42.sp
    // Hindi font sizes
    val HINDI_HERO_SIZE = 58.sp            // Size of the BIG focal Hindi word
    val HINDI_HERO_LINE_HEIGHT = 64.sp
    val HINDI_NORMAL_SIZE = 28.sp          // Size of supporting Hindi words
    val HINDI_NORMAL_LINE_HEIGHT = 36.sp
    // Left margin distance from screen edge
    val LEFT_PADDING = 36.dp
}
```


_Modify any `sp` value here to immediately change word scales, line heights, or left alignment margins._

## 2. 🔤 Font Families (Which font file is used)

Open `Type.kt` around lines 11–39:

```kotlin
// In: app/src/main/java/com/gaao/app/ui/theme/Type.kt
// Main Lyrics - Hindi: AMS Bhagyshri
val AmsBhagyshriFamily = FontFamily(
    Font(R.font.ams_bhagyshri, FontWeight.Normal)
)
// Main Lyrics - English: BasicCommerce
val BasicCommerceFamily = FontFamily(
    Font(R.font.basic_commerce_black, FontWeight.Bold)
)
// Secondary info & header text: Times New Roman
val TimesNewRomanFamily = FontFamily(
    Font(R.font.times_new_roman_condensed, FontWeight.Normal)
)
// Font selector for lyrics lines:
fun getMainLyricsFont(text: String): FontFamily {
    return if (isHindiOrDevanagari(text)) AmsBhagyshriFamily else BasicCommerceFamily
}
```

_If you ever want to change a font, drop the `.ttf` / `.otf` into `app/src/main/res/font/` (lowercase with underscores) and update the `R.font.<name>` reference here._

## 3. 🏷️ Track Header & Secondary Info Sizes

Open `GaaOScreen.kt` around lines 90–108:

- Song Title: `fontSize = 18.sp` (Line 93)
- Artist Name: `fontSize = 13.sp` (Line 103)
Both use `fontFamily = TimesNewRomanFamily`.