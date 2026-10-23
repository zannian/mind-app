# Retrofit / OkHttp / Moshi
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-keep class com.lyulf.mind.data.remote.** { *; }
-keepclassmembers class com.lyulf.mind.data.remote.** { *; }

-keep class com.squareup.moshi.** { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-keepclassmembers class ** {
    @com.squareup.moshi.* <methods>;
}

# OkHttp platform
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil
-dontwarn coil.compose.**
