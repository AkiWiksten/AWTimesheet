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
# Refined keep rules to avoid "overly broad" warnings while maintaining functionality.
# We keep specific entry points and use -keepclassmembers for internal packages.
-keep class com.google.android.libraries.places.api.Places { *; }
-keep class com.google.android.libraries.places.api.model.Place { *; }
-keep class com.google.android.libraries.places.widget.Autocomplete { *; }
-keepclassmembers class com.google.android.libraries.places.api.model.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.google.android.libraries.places.api.net.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.google.android.libraries.places.widget.** {
    <fields>;
    <init>(...);
}
-dontwarn com.google.android.libraries.places.**

# ---- Google Maps / Play Services ----
# Specific keep rules for Google Maps to avoid broad package rules
-keep class com.google.android.gms.maps.GoogleMap { *; }
-keep class com.google.android.gms.maps.SupportMapFragment { *; }
-keep class com.google.android.gms.maps.MapView { *; }
-keep class com.google.android.gms.maps.model.LatLng { *; }
-keep class com.google.android.gms.maps.model.CameraPosition { *; }
-keepclassmembers class com.google.android.gms.maps.model.** {
    <fields>;
    <init>(...);
}
-dontwarn com.google.android.gms.**
