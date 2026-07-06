# QuranMaker ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class hazem.nurmontage.videoquran.model.** { *; }
-keep class hazem.nurmontage.videoquran.entity_timeline.** { *; }
-keep class hazem.nurmontage.videoquran.common.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
-dontwarn com.pairip.**
