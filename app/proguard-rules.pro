-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-renamesourcefileattribute ''
-processkotlinnullchecks remove
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}