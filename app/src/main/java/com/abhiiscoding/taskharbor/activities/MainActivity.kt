package com.abhiiscoding.taskharbor.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.adapters.BoardItemsAdapter
import com.abhiiscoding.taskharbor.databinding.ActivityMainBinding
import com.abhiiscoding.taskharbor.databinding.NavHeaderMainBinding
import com.abhiiscoding.taskharbor.firebase.FirestoreClass
import com.abhiiscoding.taskharbor.models.Board
import com.abhiiscoding.taskharbor.models.User
import com.abhiiscoding.taskharbor.utils.Constants
import com.abhiiscoding.taskharbor.utils.SwipeToDeleteCallback
import com.abhiiscoding.taskharbor.utils.SwipeToEditCallback
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var binding: ActivityMainBinding? = null
    private var navHeaderBinding: NavHeaderMainBinding? = null

    private lateinit var mUserName: String
    private lateinit var mSharedPreferences: SharedPreferences

    companion object {
        //A unique code for starting the activity for result
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        navHeaderBinding = NavHeaderMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setupActionBar()
        binding?.navView?.setNavigationItemSelectedListener(this)

        mSharedPreferences =
            this.getSharedPreferences(Constants.TASKHARBOR_PREFERENCES, Context.MODE_PRIVATE)

        FirestoreClass().loadUserData(this, true)

        val fab: FloatingActionButton = findViewById(R.id.fab_create_board)
        fab.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

// Here, if the token is already updated, proceed with loading user details.
        if (tokenUpdated) {
            // Get the current logged in user details.
            // Show the progress dialog.
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this@MainActivity, true)
        } else {
            // Use FirebaseMessaging instead of FirebaseInstanceId
            FirebaseMessaging.getInstance().token.addOnSuccessListener(this@MainActivity) { token ->
                // Check if the token is different from the stored one
                val storedToken = mSharedPreferences.getString(Constants.FCM_TOKEN, null)
                if (token != storedToken) {
                    updateFCMToken(token)
                    saveTokenToSharedPreferences(token) // Save the new token
                } else {
                    // Token hasn't changed, proceed with loading user details
                    // Get the current logged in user details.
                    // Show the progress dialog.
                    showProgressDialog(resources.getString(R.string.please_wait))
                    FirestoreClass().loadUserData(this@MainActivity, true)
                }
            }.addOnFailureListener { e ->
                // Handle token retrieval failure
                Log.w("FCM", "Error getting token: $e")
            }
        }

    }

    private fun saveTokenToSharedPreferences(token: String) {
        mSharedPreferences.edit().putString(Constants.FCM_TOKEN, token).apply()
    }

    private fun setupActionBar() {
        setSupportActionBar(binding?.appBarMain?.toolbarMainActivity)
        binding?.appBarMain?.toolbarMainActivity?.setNavigationIcon(R.drawable.ic_navigation_menu)

        binding?.appBarMain?.toolbarMainActivity?.setNavigationOnClickListener {
            //Toggle the drawer
            toggleDrawer()
        }
    }

    fun deleteBoard(documentId: String, callback: (success: Boolean) -> Unit) {
        // Get a reference to the board document in Firestore
        val boardRef =
            FirebaseFirestore.getInstance().collection(Constants.BOARDS).document(documentId)

        // Delete the document
        boardRef.delete()
            .addOnSuccessListener {
                callback(true) // Deletion successful
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreClass", "Error deleting board: $e")
                callback(false) // Deletion failed
            }
    }


    fun populateBoardListToUI(boardsList: ArrayList<Board>) {

        hideProgressDialog()

        val rv_boards_list: RecyclerView = findViewById(R.id.rv_boards_list)
        val tv_no_boards_available: TextView = findViewById(R.id.tv_no_boards_available)
        if (boardsList.size > 0) {

            val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val adapter = rv_boards_list.adapter as BoardItemsAdapter
                    val position = viewHolder.adapterPosition

                    // Get the document ID of the board to be deleted
                    val documentId =
                        adapter.list[position].documentId

                    // Call the modified removeAt function with documentId
                    adapter.removeAt(position, documentId)
                    FirestoreClass().getBoardsList(this@MainActivity)
                }
            }
            val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
            deleteItemTouchHelper.attachToRecyclerView(rv_boards_list)


            val editSwipeHandler = object : SwipeToEditCallback(this) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val adapter = rv_boards_list.adapter as BoardItemsAdapter
                    val position = viewHolder.adapterPosition
                    adapter.notifyEditItem(
                        this@MainActivity,
                        viewHolder.adapterPosition,
                        ADD_PLACE_ACTIVITY_REQUEST_CODE
                    )
                }
            }
            val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
            editItemTouchHelper.attachToRecyclerView(rv_boards_list)



            rv_boards_list.visibility = View.VISIBLE
            tv_no_boards_available.visibility = View.GONE

            rv_boards_list.layoutManager = LinearLayoutManager(this@MainActivity)
            rv_boards_list.setHasFixedSize(true)

            // Create an instance of BoardItemsAdapter and pass the boardList to it.
            val adapter = BoardItemsAdapter(this@MainActivity, boardsList)
            rv_boards_list.adapter = adapter // Attach the adapter to the recyclerView.

            adapter.setOnClickListener(object :
                BoardItemsAdapter.OnClickListener {
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })

        } else {
            rv_boards_list.visibility = View.GONE
            tv_no_boards_available.visibility = View.VISIBLE
        }
    }

    private fun toggleDrawer() {
        if (binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            binding?.drawerLayout?.openDrawer(GravityCompat.START)
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {
        hideProgressDialog()
        mUserName = user.name

        val navView: NavigationView = findViewById(R.id.nav_view)
        // Access the header view at index 0
        val headerView: View = navView.getHeaderView(0)

        // Now you can find your views within the header view
        val navUserImage: ImageView = headerView.findViewById(R.id.nav_user_image)
        val tvUsername: TextView = headerView.findViewById(R.id.tv_username)

        Glide
            .with(this@MainActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.mipmap.ic_user_place_holder)
            .into(navUserImage)
        tvUsername.text = user.name

        if (readBoardsList) {
            Log.e("Error1", "Error while creating a board")
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this@MainActivity)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(this@MainActivity, MyProfileActivity::class.java),
                    MY_PROFILE_REQUEST_CODE
                )
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this@MainActivity, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        } else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE) {
            FirestoreClass().getBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this, userHashMap)
    }
//
//    fun deleteBoard(position: Int) {
//        val rvBoardsList: RecyclerView = findViewById(R.id.rv_boards_list)
//        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                val rvBoardsList: RecyclerView = findViewById(R.id.rv_boards_list)
//                val adapter = rvBoardsList.adapter as BoardItemsAdapter
//                adapter.removeAt(viewHolder.adapterPosition)
//            }
//        }
//
//        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
//        deleteItemTouchHelper.attachToRecyclerView(rvBoardsList)
//    }

}