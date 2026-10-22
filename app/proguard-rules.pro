# Shizuku: newProcess is reached by reflection, binders by name.
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.**
# Run via app_process by name.
-keep class com.vivo144.app.core.RmsCli { public static void main(java.lang.String[]); }
