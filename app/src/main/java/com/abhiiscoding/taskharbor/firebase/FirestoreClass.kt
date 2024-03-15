package com.abhiiscoding.taskharbor.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.abhiiscoding.taskharbor.activities.CardDetailsActivity
import com.abhiiscoding.taskharbor.activities.CreateBoardActivity
import com.abhiiscoding.taskharbor.activities.MainActivity
import com.abhiiscoding.taskharbor.activities.MembersActivity
import com.abhiiscoding.taskharbor.activities.MyProfileActivity
import com.abhiiscoding.taskharbor.activities.SignInActivity
import com.abhiiscoding.taskharbor.activities.SignUpActivity
import com.abhiiscoding.taskharbor.activities.TaskListActivity
import com.abhiiscoding.taskharbor.models.Board
import com.abhiiscoding.taskharbor.models.User
import com.abhiiscoding.taskharbor.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject

class FirestoreClass {

    // Create a instance of Firebase FireStore
    private val mFireStore = FirebaseFirestore.getInstance()

    /**
     * A function to make an entry of the registered user in the firestore database.
     */
    fun registerUser(activity: SignUpActivity, userInfo: User) {

        mFireStore.collection(Constants.USERS)
            // Document ID for users fields. Here the document it is the User ID.
            .document(getCurrentUserID())
            // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {

                // Here call a function of base activity for transferring the result to it.
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    }

    /**
     * A function to SignIn using firebase and get the user details from Firestore Database.
     */
    fun loadUserData(activity: Activity, readBoardsList: Boolean = false) {

        // Here we pass the collection name from which we wants the data.
        mFireStore.collection(Constants.USERS)
            // The document id to get the Fields of user.
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.toString())

                // Here we have received the document snapshot which is converted into the User Data model object.
                val loggedInUser = document.toObject(User::class.java)!!

                // Here call a function of base activity for transferring the result to it.
                when (activity) {
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }

                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                    }

                    is MyProfileActivity -> {
                        activity.setUserDataInUI(loggedInUser)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Here call a function of base activity for transferring the result to it.
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting loggedIn user details",
                    e
                )
            }
    }

    /**
     * A function to update the user profile data into the database.
     */
    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS) // Collection Name
            .document(getCurrentUserID()) // Document ID
            .update(userHashMap) // A hashmap of fields which are to be updated.
            .addOnSuccessListener {
                // Profile data is updated successfully.
                Log.e(activity.javaClass.simpleName, "Profile Data updated successfully!")

                Toast.makeText(activity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                when (activity) {
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }

                    is MyProfileActivity -> {
                        // Notify the success result.
                        activity.profileUpdateSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }

                    is MyProfileActivity -> {
                        // Notify the success result.
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a board.",
                    e
                )
            }
    }

    /**
     * A function for creating a board and making an entry in the database.
     */
    fun createBoard(activity: CreateBoardActivity, board: Board) {

        mFireStore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Board created successfully.")

                Toast.makeText(activity, "Board created successfully.", Toast.LENGTH_SHORT).show()

                activity.boardCreatedSuccessfully()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a board.",
                    e
                )
            }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        // The collection name for BOARDS
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get() // Will get the documents snapshots.
            .addOnSuccessListener { document ->
                // Here we get the list of boards in the form of documents.
                Log.e(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id

                activity.boardDetails(board)
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting board details.", e)
            }
    }

    /**
     * A function to get the list of created boards from the database.
     */
    fun getBoardsList(activity: MainActivity) {

        // The collection name for BOARDS
        mFireStore.collection(Constants.BOARDS)
            // A where array query as we want the list of the board in which the user is assigned. So here you can pass the current user id.
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserID())
            .get() // Will get the documents snapshots.
            .addOnSuccessListener { document ->
                // Here we get the list of boards in the form of documents.
                Log.e(activity.javaClass.simpleName, "Ban gya Hai bhyi")
                // Here we have created a new instance for Boards ArrayList.
                val boardsList: ArrayList<Board> = ArrayList()

                // A for loop as per the list of documents to convert them into Boards ArrayList.
                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardsList.add(board)
                }

                // Here pass the result to the base activity.
                activity.populateBoardListToUI(boardsList)
            }
            .addOnFailureListener { e ->

                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board222.", e)
            }
    }


    /**
     * A function for getting the user id of current logged user.
     */
    fun getCurrentUserID(): String {
        // An Instance of currentUser using FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        // A variable to assign the currentUserId if it is not null or else it will be blank.
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }

        return currentUserID
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "TaskList Updated successfully.")
                if (activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                else if (activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }.addOnFailureListener { e ->
                if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                else if (activity is CardDetailsActivity)
                    activity.hideProgressDialog()

                Log.e(activity.javaClass.simpleName, "Error while creating a taskList", e)

            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { doc ->
                Log.e(activity.javaClass.simpleName, "Members list got successfully")

                val usersList: ArrayList<User> = ArrayList()
                for (i in doc.documents) {
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if (activity is MembersActivity)
                    activity.setUpMembersList(usersList)
                if (activity is TaskListActivity)
                    activity.boardMembersDetails(usersList)
            }.addOnFailureListener { e ->
                if (activity is MembersActivity)
                    activity.hideProgressDialog()
                if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting the assigned users list",
                    e
                )
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String) {
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.documents.size > 0) {
                    val user = doc.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found!!")
                }
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting user details.",
                    e
                )
            }
    }

    fun deleteBoard(documentId: String, callback: (success: Boolean) -> Unit) {
        // Get a reference to the board document in Firestore
        val boardRef = mFireStore.collection(Constants.BOARDS).document(documentId)

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


    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User) {
        val assignedToHashMap = hashMapOf<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a board.",
                    e
                )
            }
    }


}