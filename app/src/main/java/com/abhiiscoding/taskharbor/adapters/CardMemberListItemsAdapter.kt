package com.abhiiscoding.taskharbor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.databinding.ItemCardSelectedMemberBinding
import com.bumptech.glide.Glide


open class CardMemberListItemsAdapter(
    private val context: Context,
    private val list: ArrayList<SelectedMembers>,
    private val assignedMembers: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    private lateinit var binding: ItemCardSelectedMemberBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        binding = ItemCardSelectedMemberBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            if (position == list.size - 1 && assignedMembers) {
                holder.binding.ivAddMember.visibility = View.VISIBLE
                holder.binding.ivSelectedMemberImage.visibility = View.GONE
            } else {
                holder.binding.ivAddMember.visibility = View.GONE
                holder.binding.ivSelectedMemberImage.visibility = View.VISIBLE

                Glide
                    .with(context)
                    .load(model.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(holder.binding.ivSelectedMemberImage)
            }

            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick()
                }
            }
        }
    }

    open class MyViewHolder(val binding: ItemCardSelectedMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick()
    }
}