package com.abhiiscoding.taskharbor.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.databinding.ActivityMyProfileBinding
import com.abhiiscoding.taskharbor.firebase.FirestoreClass
import com.abhiiscoding.taskharbor.models.User
import com.abhiiscoding.taskharbor.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class MyProfileActivity : BaseActivity() {
    private var binding: ActivityMyProfileBinding? = null

    //    private val userImage: ImageView = findViewById(R.id.iv_user_profile_image)
    private var mSelectedImageFileUri: Uri? = null
    private var mProfileImageUri: String = ""
    private lateinit var mUserDetails: User


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setUpActionBar()

        FirestoreClass().loadUserData(this)

        val userImage: ImageView = findViewById(R.id.iv_user_profile_image)
        userImage.setOnClickListener {
            val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    requiredPermission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Show image chooser
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(requiredPermission), Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        binding?.btnUpdate?.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            val granted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                // Show image chooser
                Constants.showImageChooser(this)
            } else {
                val requiredPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                // Provide a more informative message based on the denied permission
                Toast.makeText(
                    this,
                    "Permission for accessing ${requiredPermission.substringAfterLast('.')} is required to select images. Please enable it in settings.",
                    Toast.LENGTH_LONG
                ).show()

                // Optionally, you can also guide the user to the app settings for permission management
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null) {
            mSelectedImageFileUri = data.data
            val userImage: ImageView = findViewById(R.id.iv_user_profile_image)

            try {
                Glide.with(this@MyProfileActivity).load(mSelectedImageFileUri).centerCrop()
                    .placeholder(R.mipmap.ic_user_place_holder).into(userImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarMyProfileActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_24)
            actionBar.title = resources.getString(R.string.my_profile)
        }
        binding?.toolbarMyProfileActivity?.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun setUserDataInUI(user: User) {

        mUserDetails = user

        val userImage: ImageView = findViewById(R.id.iv_user_profile_image)
        Glide.with(this@MyProfileActivity).load(user.image).centerCrop()
            .placeholder(R.mipmap.ic_user_place_holder).into(userImage)

        binding?.etName?.setText(user.name)
        binding?.etEmail?.setText(user.email)
        if (user.mobile != 0L) {
            binding?.etMobile?.setText(user.mobile.toString())
        }

    }

    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()
        var anyChangesMade = false
        if (mProfileImageUri.isNotEmpty() && mProfileImageUri != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageUri
            anyChangesMade = true
        }
        if (binding?.etName?.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = binding?.etName?.text.toString()
            anyChangesMade = true

        }
        if (binding?.etMobile?.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = binding?.etMobile?.text.toString().toLong()
            anyChangesMade = true
        }
        if (anyChangesMade) {
            FirestoreClass().updateUserProfileData(this, userHashMap)
        }

    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))
        if (mSelectedImageFileUri != null) {
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + Constants.getFileExtension(
                    this,
                    mSelectedImageFileUri
                )
            )
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                Log.e(
                    "FirebaseImageUrl",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.e("DownloadableImageURL", uri.toString())
                    mProfileImageUri = uri.toString()
                    hideProgressDialog()

                    // TODO Update User profile data
                    updateUserProfileData()

                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this@MyProfileActivity, exception.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }


    fun profileUpdateSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

}
