package com.pant.aegispass

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Utility class to perform basic root detection checks on an Android device.
 * These checks are not foolproof and can be bypassed by advanced attackers,
 * but they provide a good first line of defense.
 */
object RootDetectionUtil {

    private val SU_BINARY_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/system/sbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/su/bin"
    )

    private val DANGEROUS_APPS_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin",
        "/system/xbin/busybox",
        "/system/bin/busybox"
    )

    /**
     * Checks if the device is rooted using various methods.
     * @return true if the device is likely rooted, false otherwise.
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3() || checkRootMethod4()
    }

    /**
     * Method 1: Check for the existence of 'su' binary in common paths.
     */
    private fun checkRootMethod1(): Boolean {
        for (path in SU_BINARY_PATHS) {
            if (File(path).exists()) {
                println("Root check method 1: Found su binary at $path")
                return true
            }
        }
        return false
    }

    /**
     * Method 2: Check for test-keys in build tags.
     * Test-keys are often used for custom ROMs or rooted devices.
     */
    private fun checkRootMethod2(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            println("Root check method 2: Found test-keys in Build.TAGS")
            return true
        }
        return false
    }

    /**
     * Method 3: Check for the existence of dangerous apps/files.
     */
    private fun checkRootMethod3(): Boolean {
        for (path in DANGEROUS_APPS_PATHS) {
            if (File(path).exists()) {
                println("Root check method 3: Found dangerous app/file at $path")
                return true
            }
        }
        return false
    }

    /**
     * Method 4: Check if 'su' command can be executed.
     */
    private fun checkRootMethod4(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = BufferedReader(InputStreamReader(process.inputStream))
            val line = `in`.readLine()
            val isRooted = line != null // If 'su' is found, line will not be null
            println("Root check method 4: su command execution result: $isRooted")
            isRooted
        } catch (e: Exception) {
            println("Root check method 4: Exception during su command execution: ${e.message}")
            false
        } finally {
            process?.destroy()
        }
    }
}