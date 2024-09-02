package com.jayesh.flutter_contact_picker

import androidx.annotation.NonNull
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.ContactsContract
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.util.ArrayList
import java.util.HashMap

/** FlutterContactPickerPlugin */
public class FlutterContactPickerPlugin: FlutterPlugin, MethodCallHandler,
  ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var channel: MethodChannel
  private var activity: Activity? = null
  private var pendingResult: Result? = null
  private val PICK_CONTACT_SINGLE = 2015
  private val PICK_CONTACT_MULTIPLE = 2016

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_native_contact_picker")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "selectContact") {
      // Single contact selection
      if (pendingResult != null) {
        pendingResult!!.error("multiple_requests", "Cancelled by a second request.", null)
        pendingResult = null
      }
      pendingResult = result

      val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
      activity?.startActivityForResult(intent, PICK_CONTACT_SINGLE)

    } else if (call.method == "selectContacts") {
      // Multiple contacts selection
      if (pendingResult != null) {
        pendingResult!!.error("multiple_requests", "Cancelled by a second request.", null)
        pendingResult = null
      }
      pendingResult = result

      val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Enable multiple contact selection
      activity?.startActivityForResult(intent, PICK_CONTACT_MULTIPLE)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(@NonNull p0: ActivityPluginBinding) {
    this.activity = p0.activity
    p0.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    this.activity = null
  }

  override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
    this.activity = activityPluginBinding.activity
    activityPluginBinding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    this.activity = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (resultCode != RESULT_OK) {
      pendingResult?.success(null)
      pendingResult = null
      return true
    }

    val contactsList = ArrayList<HashMap<String, Any>>()

    if (requestCode == PICK_CONTACT_SINGLE) {
      // Handle single contact selection
      data?.data?.let { contactUri ->
        val cursor = activity!!.contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
          it.moveToFirst()
          val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
          val fullName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
          val contact = HashMap<String, Any>()
          contact["fullName"] = fullName
          contact["phoneNumbers"] = listOf(number)
          contactsList.add(contact)
        }
      }

    } else if (requestCode == PICK_CONTACT_MULTIPLE) {
      // Handle multiple contacts selection
      val clipData = data?.clipData
      if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
          val contactUri = clipData.getItemAt(i).uri
          val cursor = activity!!.contentResolver.query(contactUri, null, null, null, null)
          cursor?.use {
            it.moveToFirst()
            val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val fullName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val contact = HashMap<String, Any>()
            contact["fullName"] = fullName
            contact["phoneNumbers"] = listOf(number)
            contactsList.add(contact)
          }
        }
      } else if (data?.data != null) {
        // Handle single contact selection in case multiple selection is not supported
        val contactUri = data.data!!
        val cursor = activity!!.contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
          it.moveToFirst()
          val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
          val fullName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
          val contact = HashMap<String, Any>()
          contact["fullName"] = fullName
          contact["phoneNumbers"] = listOf(number)
          contactsList.add(contact)
        }
      }

    } else {
      return false
    }

    pendingResult?.success(contactsList)
    pendingResult = null
    return true
  }

  companion object {
    private const val PICK_CONTACT_SINGLE = 2015
    private const val PICK_CONTACT_MULTIPLE = 2016
  }
}
