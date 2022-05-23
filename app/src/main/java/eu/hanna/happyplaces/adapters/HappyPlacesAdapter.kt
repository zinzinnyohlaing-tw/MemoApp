package eu.hanna.happyplaces.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.hanna.happyplaces.R
import eu.hanna.happyplaces.activities.AddHappyPlaceActivity
import eu.hanna.happyplaces.activities.MainActivity
import eu.hanna.happyplaces.database.DatabaseHandler
import eu.hanna.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.item_happy_place.view.*

class HappyPlacesAdapter (private val context: Context,private val list: ArrayList<HappyPlaceModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Add a variable for onClickListener interface
    private var onClickListener:OnClickListener? = null

    private class MyViewHolder (view:View) : RecyclerView.ViewHolder(view){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder (LayoutInflater.from(context).inflate(R.layout.item_happy_place,parent,false
        ))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {
            holder.itemView.iv_place_image.setImageURI(Uri.parse(model.image))
            holder.itemView.tvTitle.text = model.title
            holder.itemView.tvDescription.text = model.description

            // Add an onclickListener to the item
            holder.itemView.setOnClickListener{
                if (onClickListener!=null){
                    onClickListener?.onClick(position,model)
                }
            }
        }
    }

    override fun getItemCount(): Int {
       return list.size
    }

    // Create an interface for onclickListener
    interface OnClickListener {
        fun onClick(position: Int,model: HappyPlaceModel)
    }

    // Create a function to bind the onclickLister
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // Create a function to edit the happy place details which is inserted earlier and pass the details through intent
    fun notifyEditItem (activtiy: Activity,position: Int,requestCode: Int) {
        val intent = Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
        activtiy.startActivityForResult(intent,requestCode) // Activity is started with requestCode
        notifyItemChanged(position) // Notify any registered
        // observers that the items at position has changed
    }

    // Create a function to delete the happy place details which is inserted earlier from the local storage
    fun removeAt(position: Int) {
        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteHappyPlace(list[position])

        if (isDeleted > 0) {
            list.removeAt(position)
           notifyItemRemoved(position)
        }
    }
}