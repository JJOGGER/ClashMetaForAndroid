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

# -dontobfuscate  # 注释掉以启用混淆，但保留必要类

# ==================== Gson 混淆规则 ====================
# Keep Gson classes and methods
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep TypeToken and its subclasses
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ==================== 反射类型保护（宽松但有效）====================
# 保留反射类和接口（Gson 和 Retrofit 需要，但允许混淆）
-keep,allowobfuscation class java.lang.reflect.** { *; }
-keep,allowobfuscation interface java.lang.reflect.** { *; }

# ==================== Gson 内部类保护（宽松）====================
# 保护 Gson 内部类（允许混淆，但保留功能）
-keep,allowobfuscation class com.google.gson.internal.** { *; }

# ==================== 类型信息保留（关键属性 - 解决 ParameterizedType 错误）====================
# 保留泛型类型信息（Signature）- 这是解决 ParameterizedType 错误的最关键属性！
-keepattributes Signature
# 保留注解信息（Gson @SerializedName 等）
-keepattributes *Annotation*
# 保留内部类和封闭方法信息（TypeToken 匿名类需要）
-keepattributes InnerClasses,EnclosingMethod
# 保留运行时注解
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
# 保留异常和泛型签名信息
-keepattributes Exceptions

# Gson specific classes
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== XBoard Model 类混淆规则 ====================
# 由于上面已经保护了整个 com.xboard 包，这里只需要额外的 Gson 注解保护
# 保护所有带 @SerializedName 注解的字段名
-keepclassmembers class com.xboard.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all classes with @SerializedName annotation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== Retrofit & OkHttp 混淆规则 ====================
# Retrofit 需要的注解属性（已在上面的类型信息中保留）
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit annotations
-keepattributes *Annotation*, InnerClasses
-dontwarn retrofit2.**

# Keep OkHttp classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# ==================== EventBus 混淆规则 ====================
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# ==================== MMKV 混淆规则 ====================
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**

# ==================== XBoard 包混淆规则（直接不混淆，避免所有问题）====================
# 直接让整个 com.xboard 包不混淆，这样就能避免所有泛型类型、反射等问题
-keep class com.xboard.** { *; }
-dontwarn com.xboard.**

# 同时保护 GsonUtil（在其他包中）
-keep class com.sunmi.background.utils.GsonUtil { *; }

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkReturnedValueIsNotNull(...);
    public static void checkFieldIsNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
}

# ==================== Kotlin Coroutines 优化规则 ====================
# Allow R8 to optimize away the FastServiceLoader.
# Together with ServiceLoader optimization in R8
# this results in direct instantiation when loading Dispatchers.Main
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}

-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoaderKt {
    boolean ANDROID_DETECTED return true;
}

-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}

# Disable support for "Missing Main Dispatcher", since we always have Android main dispatcher
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatchersKt {
    boolean SUPPORT_MISSING return false;
}
# 使用R8全模式，对未保留的类剥离通用签名。挂起函数被包装在使用类型参数的continuation中。
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# 如果不保留，R8完整模式将从返回类型中剥离通用签名。
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# 在R8全模式下，对未保留的类剥离通用签名。
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# Statically turn off all debugging facilities and assertions
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
    boolean getDEBUG() return false;
    boolean getRECOVER_STACK_TRACES() return false;
}

# ==================== 额外的 Gson 规则（针对 Kotlin 数据类）====================
# 保留异常和泛型签名信息
-keepattributes Exceptions,Signature

# ==================== TypeToken 保护（Gson 需要）====================
# 保护 TypeToken 基类和子类
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# ==================== Lottie 动画库混淆规则 ====================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ==================== Glide 图片加载库混淆规则 ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ==================== AndroidX 库混淆规则 ====================
# Keep Lifecycle classes
-keep class androidx.lifecycle.** { *; }

# Keep Navigation classes
-keep class androidx.navigation.** { *; }

# Keep ViewBinding classes (generated)
-keep class * extends androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# ==================== 自定义 View 和 Adapter 保护 ====================
# Keep custom views
-keep class com.xboard.ui.round.** { *; }
-keep class com.xboard.ui.view.** { *; }
-keep class com.xboard.ui.widget.** { *; }

# Keep adapter classes
-keep class com.xboard.ui.adapter.** { *; }
-keepclassmembers class com.xboard.ui.adapter.** {
    public <methods>;
}

# ==================== WebView 相关 ====================
# Keep JavaScript interfaces
-keepclassmembers class com.xboard.ui.activity.WebViewActivity {
    @android.webkit.JavascriptInterface <methods>;
}

# ==================== Serializable 类保护 ====================
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== 额外的安全规则 ====================
# 保护所有带 @SerializedName 注解的字段名
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保护 Gson 序列化过程中使用的反射相关类
-keepclassmembers,allowobfuscation enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保护枚举类（如果模型中有枚举）
-keepclassmembers enum com.xboard.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保护嵌套类和内部类（由于整个包都不混淆，这里主要是为了兼容性）
# -keep class com.xboard.**$* { *; }  # 已通过上面的整体规则保护

# ==================== 针对特定库的额外规则 ====================
# Retrofit 响应处理
-keepattributes Exceptions

# 保留行号信息以便调试（可选）
-keepattributes SourceFile,LineNumberTable

# ==================== 修复 ParameterizedType 转换错误的关键规则 ====================
# 保护 Retrofit Converter（处理泛型类型转换）
-keep class retrofit2.converter.gson.** { *; }

# 保护 TypeToken（Gson 需要）
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# 允许 Gson 和 Retrofit 进行反射操作
-dontwarn java.lang.reflect.**