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

# Razorpay Rules
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**
-dontwarn com.google.android.apps.nbu.paisa.inapp.client.api.**
-dontwarn proguard.annotation.**

# Preserve Data Models & Firestore Deserialization
-keep class com.restroone.app.login.models.** { *; }
-keep class com.restroone.app.manager_single_res_dash.models.** { *; }
-keep class com.restroone.app.manager_single_res_dash.registration_form_screen.model.** { *; }
-keep class com.restroone.app.staff_dash.kitchen_screens.model.** { *; }
-keep class com.restroone.app.staff_dash.billing_screens.model.** { *; }
-keep class com.restroone.app.notifications.alert.** { *; }
-keep class com.restroone.app.subscription.models.** { *; }

# Gson Rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# Glide & Coil Rules
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keepclassmembers class * implements com.bumptech.glide.module.GlideModule {
  public <init>(...);
}
-dontwarn com.bumptech.glide.**