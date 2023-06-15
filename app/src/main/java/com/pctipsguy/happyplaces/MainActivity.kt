package com.pctipsguy.happyplaces

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pctipsguy.happyplaces.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val coroutineScope:CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var binding:ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val placesDao = HappyDatabase.getInstance(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.fabAddHappyPlace?.setOnClickListener {
            startActivity(Intent(this,AddHappyPlacesActivity::class.java))
        }
        coroutineScope.launch {
            placesDao.getPlacesDao().fetchAll().collect {
                if (it.isNotEmpty()) {
                    runOnUiThread {
                        binding?.rvHappyPlacesList?.visibility = View.VISIBLE
                        binding?.tvNoRecordsAvailable?.visibility = View.GONE
                    }
                    val placesAdapter = PlacesAdapter(this@MainActivity, it)
                    runOnUiThread {
                        binding?.rvHappyPlacesList?.adapter = placesAdapter
                        placesAdapter.setOnClickListener(object : PlacesAdapter.OnClickListener {
                            override fun onClick(position: Int, model: HappyPlace) {
                                val intent =
                                    Intent(this@MainActivity, HappyPlaceDetailActivity::class.java)
                                intent.putExtra("PlaceDetails", model)
                                startActivity(intent)
                            }
                        })
                        val editSwipeHandler = object : SwipeToEditCallback(this@MainActivity) {
                            override fun onSwiped(
                                viewHolder: RecyclerView.ViewHolder,
                                direction: Int
                            ) {
                                val adapter = binding?.rvHappyPlacesList?.adapter as PlacesAdapter
                                adapter.notifyEditItem(
                                    this@MainActivity,
                                    viewHolder.adapterPosition
                                )
                            }
                        }
                        val deleteSwipeHandler = object : SwipeToDeleteCallback(this@MainActivity) {
                            override fun onSwiped(
                                viewHolder: RecyclerView.ViewHolder,
                                direction: Int
                            ) {
                                val adapter = binding?.rvHappyPlacesList?.adapter as PlacesAdapter
                                adapter.notifyDeleteItem(
                                    viewHolder.adapterPosition
                                )
                            }
                        }
                        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
                        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
                        editItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
                        deleteItemTouchHelper.attachToRecyclerView(binding?.rvHappyPlacesList)
                    }
                }
                else{
                    runOnUiThread{
                    binding?.rvHappyPlacesList?.visibility = View.GONE
                    binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if(binding!=null) binding=null
        coroutineScope.cancel()
    }
}
