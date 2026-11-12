# Kestrel Courier R8 rules. No reflection except a debug-only system property read; components in the manifest are kept by AAPT2's rules.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-allowaccessmodification
-repackageclasses ''
