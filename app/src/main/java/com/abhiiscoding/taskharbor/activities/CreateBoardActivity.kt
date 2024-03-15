package com.abhiiscoding.taskharbor.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.databinding.ActivityCreateBoardBinding
import com.abhiiscoding.taskharbor.firebase.FirestoreClass
import com.abhiiscoding.taskharbor.models.Board
import com.abhiiscoding.taskharbor.utils.Constants
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class CreateBoardActivity : BaseActivity() {
    private var binding: ActivityCreateBoardBinding? = null

    private var mSelectedImageFileUri: Uri? = null

    private lateinit var mUserName: String

    private var mBoardImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME).toString()
        }

        binding?.ivBoardImage?.setOnClickListener {
            val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(
                    this, requiredPermission
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



        binding?.btnCreate?.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadBoardImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                // Call a function to update create a board.
                createBoard()
            }
        }
    }


    private fun createBoard() {
        //  A list is created to add the assigned members.
        //  This can be modified later on as of now the user itself will be the member of the board.
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID()) // adding the current user id.

        // Creating the instance of the Board and adding the values as per parameters.
        val board = Board(
            binding?.etBoardName?.text.toString(), mBoardImageUrl, mUserName, assignedUsersArrayList
        )
        FirestoreClass().createBoard(this@CreateBoardActivity, board)
    }

    private fun uploadBoardImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        //getting the storage reference
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
            "BOARD_IMAGE" + System.currentTimeMillis() + "." + Constants.getFileExtension(
                this@CreateBoardActivity,
                mSelectedImageFileUri
            )
        )

        //adding the file to reference
        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                // The image upload is success
                Log.e(
                    "Firebase Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                // Get the downloadable url from the task snapshot
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                        Log.e("Downloadable Image URL", uri.toString())

                        // assign the image url to the variable.
                        mBoardImageUrl = uri.toString()

                        // Call a function to create the board.
                        createBoard()
                    }
            }.addOnFailureListener { exception ->
                Toast.makeText(
                    this@CreateBoardActivity, exception.message, Toast.LENGTH_LONG
                ).show()

                hideProgressDialog()
            }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarCreateBoardActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_24)
            actionBar.title = resources.getString(R.string.create_board_title)
        }
        binding?.toolbarCreateBoardActivity?.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun boardCreatedSuccessfully() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null) {
            mSelectedImageFileUri = data.data
            val boardImage: ImageView = binding?.ivBoardImage!!

            try {
                Glide.with(this).load(mSelectedImageFileUri).centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder).into(boardImage)
            } catch (e: IOException) {
                e.printStackTrace()
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
}