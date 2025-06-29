# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /tools/android/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If you are using reflection or other advanced techniques, you might need to
# add more specific rules here.

# For FFmpeg-kit (if you use it and Proguard is enabled for release)
# -keep class com.arthenica.ffmpegkit.** { *; }

# For youtube-dl-android (if Proguard is enabled for release)
# You might need to add rules depending on its internal dependencies if you face issues.
# Usually, library developers provide their own proguard rules that are bundled.
# Check the youtube-dl-android documentation for any specific Proguard requirements.
