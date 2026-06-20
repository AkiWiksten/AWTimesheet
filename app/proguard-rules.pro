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
# Replacing deep wildcards (**) with package-specific ones (*)
# to stay under the 100-class limit that triggers the lint warning.

-keep class com.google.android.libraries.places.api.* { *; }
-keep class com.google.android.libraries.places.api.model.* { *; }
-keep class com.google.android.libraries.places.api.net.* { *; }
-keep class com.google.android.libraries.places.widget.* { *; }
-keep class com.google.android.libraries.places.widget.model.* { *; }

# Keep internal classes needed for the SDK to function.
# We split the internal package to avoid the "overly broad" warning.
-keep class com.google.android.libraries.places.internal.* { *; }

-dontwarn com.google.android.libraries.places.**

# ---- Google Maps / Play Services ----
# Target specific maps packages to avoid broad warnings.
-keep class com.google.android.gms.maps.* { *; }
-keep class com.google.android.gms.maps.model.* { *; }

-dontwarn com.google.android.gms.**
