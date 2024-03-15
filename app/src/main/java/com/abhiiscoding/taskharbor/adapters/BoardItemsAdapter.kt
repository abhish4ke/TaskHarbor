package com.abhiiscoding.taskharbor.adapters

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.recyclerview.widget.RecyclerView
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.activities.EditBoardActivity
import com.abhiiscoding.taskharbor.activities.MainActivity
import com.abhiiscoding.taskharbor.activities.MainActivity.Companion.EXTRA_BOARD_DETAILS
import com.abhiiscoding.taskharbor.activities.TaskListActivity
import com.abhiiscoding.taskharbor.firebase.FirestoreClass
import com.abhiiscoding.taskharbor.models.Board
import com.abhiiscoding.taskharbor.utils.Constants
import com.bumptech.glide.Glide


open class BoardItemsAdapter(
    private val context: Context,
    var list: ArrayList<Board>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    /**
     * Inflates the item views which is designed in xml layout file
     * create a new
     * {@link ViewHolder} and initializes some private fields to be used by RecyclerView.
     **/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {


        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_board,
                parent, false
            )
        )
    }

    /**
     * Binds each item in the ArrayList to a view
     *
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {

            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(holder.itemView.findViewById(R.id.iv_board_image))

            holder.itemView.findViewById<TextView>(R.id.tv_name).text = model.name
            holder.itemView.findViewById<TextView>(R.id.tv_created_by).text =
                "Created By : ${model.createdBy}"

            holder.itemView.setOnClickListener {

                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }
        }
    }

    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * A function for OnClickListener where the Interface is the expected parameter..
     */
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    /**
     * An interface for onclick items.
     */
    interface OnClickListener {
        fun onClick(position: Int, model: Board)
    }


    fun removeAt(position: Int, documentId: String) {
        if (position >= 0 && position < list.size) {
            // Show confirmation dialog before deletion
            AlertDialog.Builder(context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this board?")
                .setPositiveButton("Yes") { _, _ ->
                    // Handle Firestore deletion and local update
                    val board = list[position]

                    if (context is MainActivity) {
                        // Delete from Firestore (assuming you have a deleteBoard function)
                        context.deleteBoard(documentId) { success ->
                            if (success) {
                                // Remove board from local list and update RecyclerView
                                list.removeAt(position)
                                notifyItemRemoved(position)
                                FirestoreClass().getBoardsList(context)
                                // Optional: Show success message (e.g., Toast)
                            } else {
                                Log.e("Board deletion Unsuccessful", "Board deletion Unsuccessful")
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    fun getItemAt(position: Int): Board? {
        if (position < 0 || position >= itemCount) {
            return null
        }
        return list[position]
    }


    fun notifyEditItem(context: Context, position: Int, requestCode: Int) {
        val board = getItemAt(position) // Get the Board object based on its position
        if (board != null) {
            val intent = Intent(context, EditBoardActivity::class.java) // Replace with your desired activity for editing boards
            intent.putExtra(Constants.EXTRA_BOARD_DETAILS, board) // Pass the board details to the edit activity

        } else {
            Log.w(TAG, "Board not found at position $position")
        }
    }



    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}