package com.pctipsguy.happyplaces

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pctipsguy.happyplaces.databinding.ActivityAddHappyPlacesBinding
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddHappyPlacesActivity : AppCompatActivity() {


    private var mLocation:Location?=null
    private var mAddress:String? = null
    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10)
        .setMaxUpdates(1)
        .build()

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                val location = locationList.last()
                mLocation = location
                val addressTask =
                    GetAddressFromLatLng(this@AddHappyPlacesActivity,location.latitude, location.longitude)
                addressTask.setAddressListener(object :
                    GetAddressFromLatLng.AddressListener {
                    override fun onAddressFound(address: String?) {
                        Log.e("Address ::", "" + address)
                        mAddress = address
                    }
                    override fun onError() {
                        Log.e("Get Address ::", "Something is wrong...")
                    }
                })
                addressTask.getAddress()
            }
        }
    }

    private var binding:ActivityAddHappyPlacesBinding? =null
    private var imageURI:Uri? = null
    private var imageUri:Uri? = null
    private var mHappyPlaceDetails: HappyPlace? = null
    var customProgressDialog: Dialog? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: OnDateSetListener

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ result ->
            if(result!=null){
                imageURI = result
                binding?.ivPlaceImage?.setImageURI(imageURI!!)
            }
        }
    private val openCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
                imageURI = imageUri
                binding?.ivPlaceImage?.setImageURI(imageURI!!)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val placesDao = HappyDatabase.getInstance(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlacesBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarAddPlace)
        if(supportActionBar!=null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        if (intent.hasExtra("PlaceDetails")) {
            mHappyPlaceDetails =
                intent.getSerializableExtra("PlaceDetails") as HappyPlace
        }
        supportActionBar?.title = "Add Happy Place"

        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            binding?.ivPlaceImage?.setImageURI(mHappyPlaceDetails!!.imgUri.toUri())

            binding?.btnSave?.text = "UPDATE"
        }
        dateSetListener = OnDateSetListener{
                _,year,month,dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            binding?.etDate?.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time).toString())
        }
        binding?.etDate?.setOnClickListener{
            DatePickerDialog(this@AddHappyPlacesActivity,dateSetListener,cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding?.tvAddImage?.setOnClickListener {
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItems = arrayOf("From Gallery","Camera Capture")
            pictureDialog.setItems(pictureDialogItems){
                _,which ->
                when(which){
                    0 -> openGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> takePicture()
                }
            }
            pictureDialog.setNegativeButton("CANCEL"){dialog,_ ->
                dialog.dismiss()
            }
            pictureDialog.show()
        }
        binding?.tvSelectCurrentLocation?.setOnClickListener{
            checkLocationPermission()
            getLocation()
            binding?.etLocation?.setText(mAddress)
        }
        binding?.btnSave?.setOnClickListener {
            showProgressDialog()
            val happyPlace = HappyPlace(title = binding?.etTitle?.text.toString(),
                date=binding?.etDate?.text.toString(),
                location = mAddress!!, latitude = mLocation!!.latitude,
                longitude = mLocation!!.longitude,
                description = binding?.etDescription?.text.toString()
                , imgUri = checkImageExists(imageURI,placesDao))
            println(happyPlace)
            when(binding?.btnSave?.text){
                "SAVE" -> lifecycleScope.launch { placesDao.getPlacesDao().insert(happyPlace) }
                "UPDATE" -> {
                    happyPlace.id = mHappyPlaceDetails!!.id
                    lifecycleScope.launch { placesDao.getPlacesDao().update(happyPlace) }
                }
            }
            if(imageUri!=null) contentResolver.delete(imageUri!!,null,null)
            finish()
        }
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getLocation()
    }

    private fun saveHappyPlaceImageLocally(fileURI:Uri):Uri{
        val output = "HappyPlace" + "${UUID.randomUUID()}"+ ".jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, output)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/" + "HappyPlacesImages")
        }
        val contentResolver: ContentResolver = contentResolver
        imageURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            val inputStream = contentResolver.openInputStream(fileURI)
            val outputStream = contentResolver.openOutputStream(imageURI!!)
            inputStream?.use { input ->
                outputStream?.use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageURI!!
    }

    private fun takePicture() {
        val output = "HappyPlace" + "${UUID.randomUUID()}"+ ".png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, output)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/" + "HappyPlacesImages")
        }
        val resolver: ContentResolver = contentResolver
        imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        openCameraLauncher.launch(imageUri)
    }

    private fun checkImageExists(tempUri:Uri?, placesDao:HappyDatabase):String{
        if(tempUri!=null) {
            for (uri in placesDao.getPlacesDao().fetchAllUri()) {
                Log.e("UriCheck", tempUri.toString())
                Log.e("UriCheck", uri)
                if (tempUri.toString().takeLast(7) == uri.takeLast(7))
                    return uri
            }
            return saveHappyPlaceImageLocally(tempUri).toString()
        }
        return mHappyPlaceDetails!!.imgUri
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@AddHappyPlacesActivity)
        customProgressDialog?.setContentView(R.layout.saving_screen)
        customProgressDialog?.show()
    }

    override fun onBackPressed() {
        if(imageUri!=null) contentResolver.delete(imageUri!!,null,null)
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(binding!=null) binding=null
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun getLocation(){
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProvider?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        //Prompt the user once explanation has been shown
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        }
        if(!isLocationEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("GPS required")
                .setMessage("Please turn on GPS, to set your Location!")
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                .create()
                .show()
        }
    }


    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            99
        )
    }
}