# Regras de ProGuard/R8 para o Evolux
# Objetivo: permitir isMinifyEnabled = true sem quebrar Compose, Media3, Coil ou coroutines.

# ---- Kotlin / metadata ----
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---- Jetpack Compose ----
# Compose usa muita reflexão/composição em runtime; regras oficiais do Google.
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }
-dontwarn androidx.compose.**

# ---- Compose for TV ----
-keep class androidx.tv.** { *; }
-dontwarn androidx.tv.**

# ---- Media3 / ExoPlayer ----
# Evita quebrar decoders e extratores carregados via reflection.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---- Coil (carregamento de imagens) ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- Coroutines ----
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---- Suas próprias classes de modelo/dados (evita quebrar serialização) ----
# Ajuste o pacote abaixo se você usa serialização (ex: kotlinx.serialization, Gson)
-keep class com.evolux.tv.model.** { *; }
-keep class com.evolux.tv.data.** { *; }

# ---- Atributos gerais de debug (linha/arquivo fonte) mantidos para stack traces legíveis ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
