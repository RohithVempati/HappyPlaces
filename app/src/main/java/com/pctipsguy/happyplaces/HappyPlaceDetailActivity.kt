package com.pctipsguy.happyplaces

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.pctipsguy.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MinimapOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

class HappyPlaceDetailActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map: MapView
    private var binding: ActivityHappyPlaceDetailBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)
        var happyPlace: HappyPlace? = null
        map = binding?.map!!
        map.setTileSource(TileSourceFactory.MAPNIK)
        if (intent.hasExtra("PlaceDetails")) {
            happyPlace = intent.getSerializableExtra("PlaceDetails") as HappyPlace
        }
        if (happyPlace != null) {
            setSupportActionBar(binding?.toolbarHappyPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlace.title
            binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            binding?.ivPlaceImage?.setImageURI(happyPlace.imgUri.toUri())
            binding?.tvDescription?.text = happyPlace.description
            binding?.tvLocation?.text = happyPlace.location
        }
        val mapController = map.controller
        mapController.setZoom(20.0)
        val startPoint = GeoPoint(happyPlace!!.latitude, happyPlace.longitude)
        mapController.setCenter(startPoint)
        val compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), map)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)
        val dm : DisplayMetrics = this.resources.displayMetrics
        val minimapOverlay = MinimapOverlay(this, map.tileRequestCompleteHandler)
        minimapOverlay.setWidth(dm.widthPixels / 5)
        minimapOverlay.setHeight(dm.heightPixels / 5)
        map.overlays.add(minimapOverlay)
        //your items
        val items = ArrayList<OverlayItem>()
        items.add(OverlayItem(happyPlace.title, happyPlace.description, GeoPoint(happyPlace.latitude, happyPlace.longitude)))

//the overlay
        val overlay = ItemizedOverlayWithFocus<OverlayItem>(items, object:
            ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index:Int, item:OverlayItem):Boolean {
                //do something
                return true
            }
            override fun onItemLongPress(index:Int, item:OverlayItem):Boolean {
                return false
            }
        }, this)
        overlay.setFocusItemsOnTap(true)

        map.overlays.add(overlay)
        binding?.resetLocation?.setOnClickListener{
            mapController.animateTo(startPoint)
        }
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

}