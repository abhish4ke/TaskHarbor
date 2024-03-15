package com.abhiiscoding.taskharbor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.abhiiscoding.taskharbor.R
import com.abhiiscoding.taskharbor.databinding.ItemMemberBinding
import com.abhiiscoding.taskharbor.models.User
import com.abhiiscoding.taskharbor.utils.Constants
import com.bumptech.glide.Glide

open class MemberListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<User>
) : RecyclerView.Adapter<MemberListItemsAdapter.MyViewHolder>() {
    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]

        Glide
            .with(context)
            .load(model.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(holder.binding.ivMemberImage)

        holder.binding.tvMemberName.text = model.name
        holder.binding.tvMemberEmail.text = model.email

        if (model.selected) {
            holder.binding.ivSelectedMember.visibility = View.VISIBLE
        } else {
            holder.binding.ivSelectedMember.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                if (model.selected) {
                    onClickListener!!.onClick(position, model, Constants.UN_SELECT)
                } else {
                    onClickListener!!.onClick(position, model, Constants.SELECT)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    class MyViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    interface OnClickListener {
        fun onClick(position: Int, user: User, action: String)
    }
}

