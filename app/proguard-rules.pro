# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the default ProGuard configuration included with the Android SDK.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Data models
-keep class com.jardin.semis.data.model.** { *; }
