# Kestrel R8 rules. The app uses no reflection; activities/services declared in the manifest are kept
# automatically by AAPT2-generated rules. Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Allow R8 to be aggressive (full mode is the AGP 8 default).
-allowaccessmodification
-repackageclasses ''
