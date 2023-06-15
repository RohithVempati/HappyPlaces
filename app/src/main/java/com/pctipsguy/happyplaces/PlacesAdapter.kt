package com.pctipsguy.happyplaces

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.pctipsguy.happyplaces.databinding.ItemHappyPlaceBinding

open class PlacesAdapter(private val context: Context,private val items:List<HappyPlace>) :RecyclerView.Adapter<PlacesAdapter.ViewHolder>(){

    private var onClickListener:OnClickListener?=null


    class ViewHolder(binding:ItemHappyPlaceBinding):RecyclerView.ViewHolder(binding.root){
        val ivPlaceImage = binding.ivPlaceImage
        val tvTitle = binding.tvTitle
        val tvDescription = binding.tvDescription

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemHappyPlaceBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        holder.ivPlaceImage.setImageURI(items[position].imgUri.toUri())
        holder.tvDescription.text = items[position].description
        holder.tvTitle.text = items[position].title
        holder.itemView.setOnClickListener {
            if(onClickListener!=null){
                onClickListener!!.onClick(position, items[position])
            }
        }
    }

    fun notifyEditItem(activity: Activity,position: Int){
        val intent = Intent(context,AddHappyPlacesActivity::class.java)
        intent.putExtra("PlaceDetails",items[position])
        activity.startActivity(intent)
        notifyItemChanged(position)
    }

    fun notifyDeleteItem(position: Int){
        val placesDao = HappyDatabase.getInstance(context)
        placesDao.getPlacesDao().delete(items[position])
        notifyItemChanged(position)
    }

    interface OnClickListener{
        fun onClick(position: Int, model: HappyPlace)
    }

}