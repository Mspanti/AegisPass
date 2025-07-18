# Add project specific ProGuard rules here.
# By default, the flags set here are applied to all build variants.
# You can specify and remove flags for specific build variants in the build.gradle file.

# To enable ProGuard in your project, go to the build.gradle file in the app folder and type:
# minifyEnabled true

# Add any project specific keep options here:

# Keep all classes and methods in the PasswordCipher and PasswordHasher classes
# as they are critical for encryption/decryption and hashing.
-keep class com.pant.aegispass.PasswordCipher { *; }
-keep class com.pant.aegispass.PasswordHasher { *; }
-keep class com.pant.aegispass.SessionManager { *; }
-keep class com.pant.aegispass.PasswordGenerator { *; }
-keep class com.pant.aegispass.PasswordStrengthChecker { *; }
-keep class com.pant.aegispass.RootDetectionUtil { *; }
-keep class com.pant.aegispass.TamperDetectionUtil { *; }
-keep class com.pant.aegispass.SecurePrefsUtil { *; }
-keep class com.pant.aegispass.DataBreachChecker { *; }
-keep class com.pant.aegispass.RiskAssessor { *; }

# Keep Room entities, DAOs, and database classes
-keep class com.pant.aegispass.PasswordEntry { *; }
-keep interface com.pant.aegispass.PasswordDao { *; }
-keep class com.pant.aegispass.AppDatabase { *; }

# Keep all classes that are annotated with @Entity, @Dao, @Database for Room
-keep public class * extends androidx.room.RoomDatabase
-keep public class * extends androidx.room.Entity
-keep public class * extends androidx.room.Dao
-keep public class * extends androidx.room.Database

# Keep all classes that extend from Activity, Fragment, Service, BroadcastReceiver, etc.
# These are entry points for the Android system and cannot be obfuscated.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.lifecycle.ViewModel

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all classes that are used by data binding
-keep class androidx.databinding.ViewDataBinding { *; }
-keep class com.pant.aegispass.databinding.** { *; }
-keep public class *.databinding.** { *; }
-keep public class * implements androidx.databinding.ViewDataBinding { *; }


# Keep all classes for Material Design Components
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }

# Keep classes for BiometricPrompt
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }

# Keep classes for OkHttp and its dependencies
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# Keep classes for jBCrypt
-keep class org.mindrot.jbcrypt.BCrypt { *; }

# Keep classes for AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }
-keep interface androidx.security.crypto.** { *; }

# Keep classes for Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }

# Keep enum values and methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# For libraries that use reflection (e.g., Gson, Retrofit if used)
# You might need to add specific rules based on the libraries you use.
# Example for Gson (if you used it for JSON parsing):
#-keep class com.example.MyDataModel { *; }
#-keepclassmembers class com.example.MyDataModel {
#    <fields>;
#}

# If you use Kotlin reflection, you might need to add:
#-dontwarn kotlin.reflect.**
#-keep class kotlin.reflect.** { *; }

# For BuildConfig
-keep class **.BuildConfig { *; }

# If you encounter issues, you can temporarily disable obfuscation for certain parts
# or add more specific -keep rules.