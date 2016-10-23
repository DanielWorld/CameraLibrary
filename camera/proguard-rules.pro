#http://proguard.sourceforge.net/manual/examples.html
#http://proguard.sourceforge.net/manual/usage.html

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,LineNumberTable,*Annotation*,EnclosingMethod

-keepclasseswithmembers class * {
	native <methods>;
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclasseswithmembers class com.danielpark.camera.util.CameraLogger {
    public <methods>;
}

-keepclasseswithmembers class com.danielpark.camera.util.AutoFitTextureView {
    public <methods>;
}

-keepclasseswithmembers class com.danielpark.camera.CameraApiChecker {
   public static synchronized com.danielpark.camera.CameraApiChecker getInstance();
   public com.danielpark.camera.CameraApiChecker setOrientation(int);
   public com.danielpark.camera.util.AutoFitTextureView build(android.app.Activity);
}

-keepclasseswithmembers interface com.danielpark.camera.listeners.ControlInterface {
    <methods>;
}
-keepclasseswithmembers interface com.danielpark.camera.listeners.OnTakePictureListener {
    <methods>;
}

-keepclasseswithmembers class com.danielpark.camera.CameraPreview {
    public <methods>;
}
-keepclasseswithmembers class com.danielpark.camera.Camera2Preview {
    public <methods>;
}