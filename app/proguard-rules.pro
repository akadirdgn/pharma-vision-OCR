# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes
-keep class com.kadir.bitirme.data.model.** { *; }

# Keep database helpers
-keep class com.kadir.bitirme.data.local.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# TextToSpeech
-keep class android.speech.tts.** { *; }
