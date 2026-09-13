# NOVA Mobile ProGuard rules

# Keep Room entities/DAOs metadata
-keep class hu.nova.mobile.database.entity.** { *; }
-keep interface hu.nova.mobile.database.dao.** { *; }

# Keep AI provider implementations reachable via reflection-free factory (not strictly
# required, kept for clarity and future-proofing if a plugin system is added).
-keep class hu.nova.mobile.ai.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer {
    *** serializer(...);
}
