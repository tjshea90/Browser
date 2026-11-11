# GeckoView ships its own consumer rules (proguard.txt in the AAR). Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-allowaccessmodification
# The excluded Google Play services FIDO classes are only referenced by WebAuthn code paths that cannot run here.
-dontwarn com.google.android.gms.**
-dontwarn org.mozilla.geckoview.**

# GeckoView logs every native message with Log.d; the extension bridge sends one per request. Verbose/debug logging has no place in a release.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
