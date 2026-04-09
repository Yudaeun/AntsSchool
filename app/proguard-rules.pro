# 스택 트레이스 디버깅용 라인 정보 유지
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Retrofit + OkHttp ──────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# ── Gson (DTO 직렬화/역직렬화) ──────────────────────────────────────────────
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 네트워크 DTO 클래스 (Gson이 리플렉션으로 접근)
-keep class com.day.antsschool.network.dto.** { *; }

# ── Firebase / Google Services ─────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Jetpack Compose ────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── AdMob ──────────────────────────────────────────────────────────────────
-keep public class com.google.android.gms.ads.** { public *; }

# ── Coroutines ─────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
