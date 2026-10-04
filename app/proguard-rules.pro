# Load Tracker Pro - Release ProGuard & R8 Optimization Keep Rules

# Preserve line numbers and source file names for Play Console crash symbolication
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Room Database Entities, DAOs, and Migrations
-keep class com.loadtracker.pro.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}

# Google ML Kit Text Recognition OCR
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Play Services & Maps
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
