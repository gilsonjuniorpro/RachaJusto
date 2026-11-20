# ProGuard rules for RachaJusto
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rachajusto.data.**$$serializer { *; }
-keepclassmembers class com.rachajusto.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.rachajusto.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
