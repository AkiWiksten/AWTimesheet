# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- Google Places SDK ----
# Keep the public API entry points and widget (AutocompleteActivity)
-keep class com.google.android.libraries.places.api.Places { *; }
-keep class com.google.android.libraries.places.api.model.** { *; }
-keep class com.google.android.libraries.places.api.net.** { *; }
-keep class com.google.android.libraries.places.widget.** { *; }
-keepnames class com.google.android.libraries.places.** { *; }
-dontwarn com.google.android.libraries.places.**

# ---- Google Maps / Play Services ----
# Keep GMS Tasks and Maps classes used reflectively at runtime
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**
