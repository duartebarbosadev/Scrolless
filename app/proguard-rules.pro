# Okio
-dontwarn java.nio.file.*
-dontwarn okio.**

# OkHttp
-keepattributes Signature
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# crashlytics recommendations
-keepattributes *Annotation*
-keepattributes LineNumberTable
-keep public class * extends java.lang.Exception


# project
-keep class com.scrolless.framework.** { *; }

# Preserve all fundamental application classes.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

-keepclassmembers class ** implements androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);

    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);

    public static ** inflate(android.view.LayoutInflater, android.view.ViewGroup);
}