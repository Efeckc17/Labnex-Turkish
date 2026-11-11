-allowaccessmodification
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all classes and fields that use volatile state variables
-keepclassmembers class * {
    volatile <fields>;
}

# Suppress warnings for libraries that ship without ProGuard rules
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.gson.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep AndroidX Lifecycle, SavedState, and Core
-keep class androidx.savedstate.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit & Networking
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# App helper & core application
-keep class com.labnex.app.helpers.** { *; }
-keep class com.labnex.app.core.CoreApplication { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep interface com.labnex.app.database.api.** { *; }
-keep class com.labnex.app.database.api.** { *; }
-keepattributes *Annotation*
-dontwarn androidx.room.paging.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**
