# GeckoView ships its own consumer rules (proguard.txt in the AAR). Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-allowaccessmodification
# The excluded Google Play services FIDO classes are only referenced by WebAuthn code paths that cannot run here.
-dontwarn com.google.android.gms.**
-dontwarn org.mozilla.geckoview.**
