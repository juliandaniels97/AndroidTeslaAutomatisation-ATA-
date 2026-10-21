-keep class org.tfv.ata.core.** { *; }
-keepclassmembers class * { @kotlinx.serialization.* <fields>; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }

# AGP 9 / R8: keep constructors that libraries create via reflection.
-keep class * implements androidx.startup.Initializer { <init>(); }
