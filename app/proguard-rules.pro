# Add project specific ProGuard rules here.
# Keep the contract data classes for Gson serialization
-keep class dev.aperture.core.contract.** { *; }
-keepclassmembers class dev.aperture.core.contract.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
