package eu.hanna.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
//import android.location.LocationRequest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import eu.hanna.happyplaces.R
import eu.hanna.happyplaces.database.DatabaseHandler
import eu.hanna.happyplaces.models.HappyPlaceModel
import eu.hanna.happyplaces.utils.GetAddressFromLatLng
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    // A variable to get an instance calendar using the default time zone and locale
    private var cal = Calendar.getInstance()

    // A variable for DatePickerDialog OnDateSetListener
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    //Now as per our Data Model Class we need some of the values to be passed so let us create that global which will be used later on.)
    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    // A variable for data model class in which we will receive the details to edit
    private var mHappyPlaceDetails: HappyPlaceModel? = null

    // A variablr for FusedLocationProviderClient which is later used to get the current location
    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)

        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        // Initialize the Fused Location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Assign the details to the variable of data modle class which we have created above the details which we will receive through intent
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
                mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
            }
        }

        dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_YEAR, dayOfMonth)
                updateDateInView()
            }

        //Here instead of validating the date we can set the current date to the view and user can change if needed.
        updateDateInView()

        // Filling the existing details to the UI components to edit
        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_location.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitutde

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            iv_place_image.setImageURI(saveImageToInternalStorage)

            btn_save.text = "UPDATE"

        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        tv_select_current_location.setOnClickListener(this)
    }

    private fun updateDateInView() {
        val myFormat = "yyyy/MM/dd" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault()) // A date format
        et_date.setText(sdf.format(cal.time).toString())
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener, // This is the variable which have created globally and initialized in setupUI method.
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR), // Here the cal instance is created globally and used everywhere in the class where it is required.
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems =
                    arrayOf("Select photo from gallery", "Capture photo from camera")
                pictureDialog.setItems(
                    pictureDialogItems
                ) { dialog, which ->
                    when (which) {
                        // Here we have create the methods for image selection from GALLERY
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }

            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // This will redirect you to setting from where you need to turn on the location provider
                    val intent = Intent (Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    Dexter.withActivity(this)
                        .withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ). withListener(
                            object : MultiplePermissionsListener {
                                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                    if (report!!.areAllPermissionsGranted()) {
                                       // Toast.makeText(this@AddHappyPlaceActivity, "Your location provider is turned off. Please turn it on.", Toast.LENGTH_SHORT).show()
                                        requestNewLocationData()
                                    }
                                }

                                override fun onPermissionRationaleShouldBeShown(
                                    permissions: MutableList<PermissionRequest>?,
                                    token: PermissionToken?
                                ) {
                                    showRationalDialogForPermissions()
                                }
                            }
                        ).onSameThread().check()
                }
            }

            R.id.btn_save -> {
               when {
                   et_title.text.isNullOrEmpty() -> {
                       Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                   } et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                   } et_location.text.isNullOrEmpty() -> {
                         Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show()
                   } saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                   }
                   else -> {

                       // Assigning all the values to data model class
                       val happyPlaceModel = HappyPlaceModel(
                           // Changing the id if it is for edit
                       if (mHappyPlaceDetails == null ) 0 else mHappyPlaceDetails!!.id,
                           et_title.text.toString(),
                           saveImageToInternalStorage.toString(),
                           et_description.text.toString(),
                           et_date.text.toString(),
                           et_location.text.toString(),
                           mLatitude,
                           mLongitude
                       )

                       val dbHandler = DatabaseHandler(this)
                       if (mHappyPlaceDetails == null) {
                           val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                           if (addHappyPlace > 0 ){
                               setResult(Activity.RESULT_OK)
                               finish()
                           }
                       } else {
                           val addHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                           if (addHappyPlace > 0 ){
                               setResult(Activity.RESULT_OK)
                               finish()
                           }
                       }

                   }
               }

            }
        }
    }

    // A function to request the current location. Using the fused location provider client
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

      //  mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    // Create a lcoation callback object of fused location provider client where we will get the current location details
    private val mLocationCallback = object:LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
            mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

            // Call the AsyncTask class for getting an address from the latitude and longitude
            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity,mLatitude,mLongitude)
            addressTask.setAddressListener(object:GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    et_location.setText(address) // Address is set to the edittext
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something is wrong...")
                }

            })
            addressTask.getAddress()
        }
    }

    // A function which is used to verify that the location or let's GPS is enable or not of the user device
    private fun isLocationEnabled() : Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //A method is used for image selection from GALLERY / PHOTOS of phone storage.
    private fun  choosePhotoFromGallery() {
        // Asking the permissions of Storage using DEXTER Library which we have added in gradle file.)
        // START
        Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    // Here after all the permission are granted launch the gallery to select and image.
                    if (report!!.areAllPermissionsGranted()) {
                        // Adding an image selection code from Gallery or phone storage
                        val galleryIntent = Intent (Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, GALLERY)
                        //Toast.makeText(this@AddHappyPlaceActivity,"Storage READ/WRITE permission are granted. Now you can select an image from GALLERY or lets says phone storage.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()
        // END
    }

    // A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
    private fun showRationalDialogForPermissions() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    // Receive the result of Gallery and Camera
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                if (data != null) {
                    val contentURI = data.data
                    try{
                        // Here this is used to get an bitmap from URI
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                        // Saving an image which is selected from Gallery and printed the path in logcat
                        // make this uri path global so we can save it to our local database
                         saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.d("Saved Image : ", "Path :: $saveImageToInternalStorage")

                        iv_place_image.setImageBitmap(selectedImageBitmap)

                    } catch(e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this@AddHappyPlaceActivity,"Failed",Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (requestCode == CAMERA) {
                val thumbnail : Bitmap = data!!.extras!!.get("data") as Bitmap  // Bitmap from Camera

                // Saving an image which is selected from Gallery and printed the path in logcat
                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.d("Saved Image : ", "Path :: $saveImageToInternalStorage")

                iv_place_image.setImageBitmap(thumbnail)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("Cancelled", "Cancelled")
        }
    }

    // Creating a method for image capturing and selecting from camera
    private fun takePhotoFromCamera() {
        Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(intent, CAMERA)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    // A function to save a copy of an image to internal storage for HappyPlaceApp to use.
    private fun saveImageToInternalStorage (bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File (file,"${UUID.randomUUID()}.jpg") // Create a file to save the image

        try{
            // Get the file output stream
            val stream : OutputStream = FileOutputStream (file)

            // Compress bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)

            // flush the stream
            stream.flush()
            stream.close()
        } catch (e:IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlaceAppImages"  // Create an const variable to use for directory name for copying the selected image
    }
}