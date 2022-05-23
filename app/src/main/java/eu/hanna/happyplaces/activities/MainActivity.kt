package eu.hanna.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.hanna.happyplaces.R
import eu.hanna.happyplaces.adapters.HappyPlacesAdapter
import eu.hanna.happyplaces.database.DatabaseHandler
import eu.hanna.happyplaces.models.HappyPlaceModel
import eu.hanna.happyplaces.utils.SwipeToDeleteCallback
import eu.hanna.happyplaces.utils.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fabAddHappyPlace.setOnClickListener {
            val intent = Intent (this@MainActivity, AddHappyPlaceActivity::class.java)
           startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }
        getHappyPlacesListFromLocalDB()
    }
    // A function to get the list of happy place form local database
    private fun getHappyPlacesListFromLocalDB () {
        val dbHandler = DatabaseHandler (this)
        val getHappyPlaceList = dbHandler.getHappyPlacesList()
        if (getHappyPlaceList.size > 0) {
            rv_happy_places_list.visibility = View.VISIBLE
            tv_no_records_available.visibility = View.GONE
            setupHappyPlacesRecyclerView(getHappyPlaceList)
        } else {
            rv_happy_places_list.visibility = View.GONE
            tv_no_records_available.visibility = View.VISIBLE
        }
    }

    //A function to populate the recyclerview to the UI.
    private fun setupHappyPlacesRecyclerView (happyPlacesList:ArrayList<HappyPlaceModel>) {
        /*rv_happy_places_list.layoutManager = GridLayoutManager (this,2)
        rv_happy_places_list.setHasFixedSize(true)
        val placeAdapter = HappyPlacesAdapter (this,happyPlacesList)
        rv_happy_places_list.adapter = placeAdapter*/

        rv_happy_places_list.layoutManager = LinearLayoutManager(this)
        rv_happy_places_list.setHasFixedSize(true)

        val placesAdapter = HappyPlacesAdapter(this, happyPlacesList)
        rv_happy_places_list.adapter = placesAdapter

        // Bind the onClickListener with adapter onClick function
        placesAdapter.setOnClickListener(object : HappyPlacesAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity,HappyPlaceDetailsActivity::class.java)

                // Pass the HappyPlaceDetails data model class to the detail activity
                intent.putExtra(EXTRA_PLACE_DETAILS, model) // Passing the complete serializable data class to the detail activity using intent.
                startActivity(intent)
            }
        })

        // Bind the edit feature class to recyclerview
        val editSwipHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_places_list.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipHandler)
        editItemTouchHelper.attachToRecyclerView(rv_happy_places_list)

        // Bind the delete feature class to recyclerview
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_places_list.adapter as HappyPlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                getHappyPlacesListFromLocalDB() // Gets the latest list from the local database after item being delete from it

            }
        }
        val deleteTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteTouchHelper.attachToRecyclerView(rv_happy_places_list)
    }

    // Call Back method to get the message from other activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                getHappyPlacesListFromLocalDB()
            } else{
                Log.e("Activity", "Cancelled or Back Pressed")
            }
        }
    }


    companion object{
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1

        //Create a constant which will be used to put and get the data using intent from one activity to another.)
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}