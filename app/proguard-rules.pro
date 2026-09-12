# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path/to/proguard-android-optimize.txt

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
-keepclassmembers class **$Companion {
    public synthetic <fields>;
}

# Keep Data Models
-keep class com.zaaam.zreming.data.model.** { *; }
-keep class com.zaaam.zreming.domain.model.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes Exceptions
