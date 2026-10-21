-keep class org.tfv.ata.core.** { *; }
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keepclassmembers class * { @kotlinx.serialization.* <fields>; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }

# AGP 9 / R8 strips constructors that libraries only reach via reflection. WorkManager creates its
# Room database (WorkDatabase_Impl) and the androidx.startup initializers reflectively at app start;
# without these rules the app crashes immediately (v1.3.1).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * implements androidx.startup.Initializer { <init>(); }
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { <init>(); }
