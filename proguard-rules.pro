# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep TrustPin SDK classes
-keep class cloud.trustpin.kotlin.sdk.** { *; }

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep kotlinx.coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }

# Common Android rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses