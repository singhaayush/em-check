package com.example.emulatordetection

import android.content.Context
import android.content.DialogInterface
import android.net.ParseException
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.EnvironmentCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.safetynet.SafetyNet
import com.nimbusds.jose.JWSObject
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (isEmulator(this) || isRooted(this))
            alertBoxEmulator()
        else
            checkPlayServiceVersion()

    }

    private fun alertBoxEmulator() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("This app don't work with emulator or rooted device.")
            .setCancelable(false)
            .setPositiveButton(
                "Okay"
            ) { _, _ -> finish() }
            .create()
        alertDialog.show()
    }


    private fun isEmulator(context: Context): Boolean {
        val androidId = Settings.Secure.getString(context.contentResolver, "android_id")
        return ("sdk" == Build.PRODUCT || "google_sdk" == Build.PRODUCT || androidId == null || Build.FINGERPRINT.startsWith(
            "generic"
        )
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith(EnvironmentCompat.MEDIA_UNKNOWN))


    }

    private fun isRooted(context: Context?): Boolean {
        val isEmulator: Boolean = isEmulator(this)
        val buildTags = Build.TAGS
        return if (!isEmulator && buildTags != null && buildTags.contains("test-keys")) {
            true
        } else {
            var file = File("/system/app/Superuser.apk")
            if (file.exists()) {
                true
            } else {
                file = File("/system/xbin/su")
                !isEmulator && file.exists()
            }
        }
    }

    private fun checkPlayServiceVersion() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
            == ConnectionResult.SUCCESS
        ) {
            Log.d(TAG, "checkPlayServiceVersion: The SafetyNet Attestation API is available.")
            checkDeviceIntegrity()
        } else {
            // "checkPlayServiceVersion: Prompt user to update Google Play services."
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Warning")
                .setCancelable(false)
                .setMessage("Please Update your gPlay servives")
                .setPositiveButton(
                    "Okay"
                ) { _, _ -> finish() }
            alertDialog.show()

        }
    }

    private fun checkDeviceIntegrity() {

        //bad way of generating nounce and passik api_key
        SafetyNet.getClient(this).attest(
            "R2Rra24fVm5xa2Mg".toByteArray(),  //TODO create by using user data and timestamp
            "AIzaSyB7U2sPFWUGzbEmC5B7bA4utLcc6rJVTYQ" //TODO should be fetched by api
        )
            .addOnSuccessListener(this) {
                // Indicates communication with the service was successful.
                // Use response.getJwsResult() to get the result data.

                try {
                    val jwsObject: JWSObject = JWSObject.parse(it.jwsResult)
                    println("header = " + jwsObject.header)
                    println("header = " + jwsObject.header.x509CertChain)
                    println("signature = " + jwsObject.signature)
                    println("signature = " + jwsObject.signature.decodeToString())
                    println("payload = " + jwsObject.payload.toJSONObject())

                    tv_response.text =
                        jwsObject.payload.toJSONObject().getAsString("ctsProfileMatch")


                    val error = jwsObject.payload.toJSONObject().getAsString("error")?.toString()
                    val apkCertificateDigestSha256 =
                        jwsObject.payload.toJSONObject().getAsString("apkCertificateDigestSha256")
                            ?.toString()

                    if (error == "internal_error" && apkCertificateDigestSha256 == "[]") {
                        alertBoxEmulator()
                    }

                    if (TextUtils.isEmpty(tv_response.text))
                        tv_response.text = "This Device failed Integrity Test"
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }
            .addOnFailureListener(this) { e ->
                // An error occurred while communicating with the service.
                if (e is ApiException) {
                    // An error with the Google Play services API contains some
                    // additional details.
                    val apiException = e as ApiException

                    // You can retrieve the status code using the
                    // apiException.statusCode property.
                    Log.d(TAG, "checkDeviceIntegrity: ${e.statusCode}")
                } else {
                    Log.d(TAG, "checkDeviceIntegrity: A different, unknown type of error occurred.")

                }
            }
    }

}
