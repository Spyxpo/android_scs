# SCS SDK ProGuard Rules

# Keep SCS SDK classes
-keep class com.spyxpo.scs.** { *; }
-keepclassmembers class com.spyxpo.scs.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Keep model classes for JSON serialization
-keep class com.spyxpo.scs.models.** { *; }
-keepclassmembers class com.spyxpo.scs.models.** { *; }
