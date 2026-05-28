# Keep our package intact to prevent any Room reflection or ViewModel crashes
-keep class com.example.qrcodescanner.** { *; }
-keep interface com.example.qrcodescanner.** { *; }

# Room Database runtime and generated class rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase$* { *; }
-dontwarn androidx.room.**

# Keep ML Kit and Google Play Services to prevent Barcode Scanning JNI crashes
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep CameraX JNI and provider classes
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep ZXing classes
-keep class com.google.zxing.** { *; }

# Keep standard Android lifecycle & ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
# JavaMail rules
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.activation.** { *; }
-dontwarn java.awt.**
-dontwarn javax.activation.**
-dontwarn javax.mail.**
