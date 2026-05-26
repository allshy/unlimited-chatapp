-keep,allowobfuscation,allowshrinking class kotlinx.serialization.Serializer
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.wuxianzhi.chat.** {
    kotlinx.serialization.KSerializer serializer(...);
}
