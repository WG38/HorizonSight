package com.example.horizonsight

//import java.awt.PageAttributes.MediaType

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.horizonsight.BuildConfig.server_api_key
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlin.math.*
import kotlin.properties.Delegates


var mMap: GoogleMap? = null
lateinit var currAltHolder : String

//variables for determining the precision of our horizon scan
var nOfRayHolder = 15
var nOfSamplesHolder = 10
var startAngleHolder = 0.0
var endAngleHolder = 360.0
val horizonCalcScope = CoroutineScope(Dispatchers.IO + CoroutineName("Horizon Coroutine"))
val altitudeCalcScope = CoroutineScope(Dispatchers.IO + CoroutineName("Altitude Coroutine"))
lateinit var tempPolylineHolder : Polyline

class MainActivity : AppCompatActivity(), OnMapReadyCallback{
    //variables for location & altitude handling
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locArray : Array<Double>
    private var altitudeHolder by Delegates.notNull<Double>()
    private var horizonHolder by Delegates.notNull<Double>()
    private var artificialHorizonHolder by Delegates.notNull<Double>()
    private var artificialHorizonCheck = false
    private lateinit var artificiaLocationHolder : Array<Double>
    private var artificialLocationCheck = false
    private var constantLocationRequests = false


    //locate button
    private lateinit var snapLocation: Button
    private var mCurrLocationMarker: Marker? = null

    //display horizon button
    private var mHorizonCircle : Circle? = null
    private lateinit var displayHorizonButton: Button
    private var mHorizonPolyline : Polyline? = null


    //horizon scanner settings
    private lateinit var enterSetMenuButton : Button






    //todo: continuous horizon/location display after first "locate" is clicked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this!!)


        //initialize current location & altitude variables
        locArray = arrayOf(0.0,0.0)
        altitudeHolder = 0.0
        horizonHolder = 0.0
        currAltHolder = ""
        artificialHorizonHolder = 0.0
        artificialHorizonCheck = false
        artificiaLocationHolder = arrayOf(0.0,0.0)


        //give colors to buttons


        //get initial location
        isLocationPermissionGranted()
        if (!artificialLocationCheck) {
            getLastKnownLocation()
        }
        else {
            locArray = artificiaLocationHolder
        }





        //gets the onMapReady function
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        snapLocation = findViewById(R.id.snapLocButton)
        snapLocation.setBackgroundColor(Color.RED)
        snapLocation.setOnClickListener() {
            if (constantLocationRequests) {
                constantLocationRequests = false
                //getLastKnownLocation()
                //  DisplayLocation(locArray[0],locArray[1])
                snapLocation.setBackgroundColor(Color.RED)

            }
            else {
                constantLocationRequests = true
                snapLocation.setBackgroundColor(Color.GREEN)
                if (!artificialLocationCheck) {
                    getLastKnownLocation()
                }
                else {
                    snapToLocation(locArray)
                }

            }



        }



        displayHorizonButton = findViewById(R.id.disHozButton)
        displayHorizonButton.setBackgroundColor(Color.WHITE)
        displayHorizonButton.setOnClickListener() {
            distanceToHorizon(altitudeHolder,locArray)

            //change this
            if (artificialHorizonCheck) {
                displayHorizon(artificialHorizonHolder,locArray)
            }
            else
            {
                displayHorizon(horizonHolder,locArray)

            }


        }

        //enter settings tab
        enterSetMenuButton = findViewById(R.id.settButton)
        enterSetMenuButton.setBackgroundColor(Color.WHITE)
        enterSetMenuButton.setOnClickListener() {
            openSettingsActivity()

        }

        //get values from search menu
        val intent = intent
        val temp_ray = intent.getStringExtra("ray_value")
        val temp_sample = intent.getStringExtra("sample_value")
        val temp_start_angle = intent.getStringExtra("start_angle")
        val temp_end_angle = intent.getStringExtra("end_angle")
        if (temp_ray != null && temp_sample != null && temp_start_angle != null && temp_end_angle != null) {
            nOfRayHolder  = temp_ray.toInt()
            nOfSamplesHolder = temp_sample.toInt()
            startAngleHolder = temp_start_angle.toDouble()
            endAngleHolder = temp_end_angle.toDouble()
            artificialHorizonHolder = intent.getStringExtra("artf_horizon")!!.toDouble()
            artificialHorizonCheck = intent.getStringExtra("artf_horizon_check").toBoolean()
            artificiaLocationHolder = arrayOf(intent.getStringExtra("artf_lat")!!.toDouble(),intent.getStringExtra("artf_lon")!!.toDouble())
            artificialLocationCheck = intent.getStringExtra("artf_loc_check").toBoolean()
        }



        //mMap = temporaryMap.









    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()


    }
    override fun onResume() {
        super.onResume()
        if (!artificialLocationCheck) {
            getLastKnownLocation()
            startLocationUpdates()
        }
        else {
            DisplayLocation(artificiaLocationHolder[0],artificiaLocationHolder[1])
        }








    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


    }

    private fun openSettingsActivity()
    {
        val intent = Intent(this,SettingsActivity::class.java)
        startActivity(intent)
    }


    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),1

            )
            false
        } else {
            true
        }
    }
    private fun getLastKnownLocation(){ //myCallback: (Double) -> Double
        var mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this!!)
        //var lat:Double = 4.0
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {


        }
        locationRequest = LocationRequest.create()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 100
        locationRequest.smallestDisplacement = 1f // 170 m = 0.1 mile
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                val location = locationResult.lastLocation
                val lat = location?.latitude
                val lon = location?.longitude


                if (lat != null) {
                    if (lon != null) {


                        DisplayLocation(lat,lon)


                    }
                }




            }
        }
    }
    fun DisplayLocation(latitude:Double,longitude:Double)
    {
        val current_location_str = latitude.toString() + " " + longitude.toString()
        val current_location_text = findViewById<View>(R.id.locText) as TextView






        val  current_altitude_str = altitudeHolder.toString()
        val horizon_dist = horizonHolder


        current_location_text.text = "Current Location: $current_location_str \n Current Altitude: $current_altitude_str m"

        val current_horizon_text = findViewById<View>(R.id.horizonText) as TextView
        current_horizon_text.text = "Current Horizon Distance: $horizon_dist km"

        locArray = arrayOf<Double>(latitude,longitude)

        if (constantLocationRequests){
            snapToLocation(locArray)

        }

        //altitudeHolder = altitude




    }
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)


    }
    private fun distanceToHorizon(alt: Double, latlong: Array<Double>) {

        altitudeCalcScope.launch {
            val altURL = URL("https://maps.googleapis.com/maps/api/elevation/json?locations=${latlong[0]}%2C${latlong[1]}&key=${server_api_key}")
            val connection = altURL.openConnection()
            BufferedReader(InputStreamReader(connection.getInputStream())).use { inp ->
                var line: String?


                while (inp.readLine().also { line = it } != null) {

                    if (line?.contains("elevation") == true) {
                        var altitudeStr = line.toString()
                        altitudeStr =  altitudeStr.subSequence(23,altitudeStr.length-1) as String
                        altitudeHolder = altitudeStr.toDouble()
                    }

                }

            }

        }
        horizonHolder =  3.57 * sqrt(altitudeHolder)


    }
    fun snapToLocation(latlong : Array<Double>)
    {
        mCurrLocationMarker?.remove()
        val lat = latlong[0]
        val lon = latlong[1]
        val latlng = LatLng(lat,lon)
        val markerOptions = MarkerOptions()
        markerOptions.position(latlng)
        markerOptions.title("Current Position")

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        mCurrLocationMarker = mMap?.addMarker(markerOptions)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 14.0f))
    }
    fun displayHorizon(horizonDist : Double,latlong:Array<Double>)
    {




        mHorizonPolyline?.remove()
        mHorizonCircle?.remove()
        val lat = latlong[0]
        val lon = latlong[1]
        val latlng = LatLng(lat,lon) // later fuse this with the calculation from snapToLocation and place in 1 place




        //all the magic point calculation happens here (inside a coroutine in another scope)
        horizonCalcScope.launch {
            val allLatLng = asyncAltitudeRequester(latlong,horizonDist)

            var finalLatLngArray = mutableListOf<LatLng>()
            for (i in 0 .. allLatLng.size-1) {
                finalLatLngArray.add(LatLng(allLatLng[i][0],allLatLng[i][1]))
            }

            //and we draw the actual lines here:
            runOnUiThread() {
                val horizonPolyLine = mMap?.addPolyline(PolylineOptions()
                    .clickable(false)
                    .add(
                        finalLatLngArray[0]))
                for (i in 0 .. finalLatLngArray.size-1) {
                    if (horizonPolyLine != null) {
                        val omg = horizonPolyLine.points
                        omg.add(finalLatLngArray[i])
                        horizonPolyLine.points = omg
                    }
                }
            }



        }



        mHorizonCircle = mMap?.addCircle(
            CircleOptions()
                .center(latlng)
                .radius(horizonDist*1000)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#2271cce7"))
        )
    }
    suspend fun asyncAltitudeRequester(latLonHolder : Array<Double>, horizonDist : Double) : MutableList<Array<Double>> {

        //ok so AltitudeAsyncTasks takes our current position,altitude and
        // horizon distance and based on it iterates through n km increments in i directions to find if theres a higher elevation
        // than location elevation, that obstructs the horizon

        val masterAltHolder = arrayOf(latLonHolder[0],latLonHolder[1],altitudeHolder,horizonDist)
        //val altTask =  AltitudeAsyncTasks()
            //val myTaskParams = arrayOf(true, true, true)
        val myAsyncTask = doInBackground(masterAltHolder)//execute(masterAltHolder).get()
        return myAsyncTask










    }
    //class AsyncHorizonClass {//AsyncTask<Array<Double>,Unit, MutableList<Array<Double>>>() {


        private fun doInBackground(vararg combinedLatLonAltHozArray: Array<Double>): MutableList<Array<Double>> {


            val latitude = combinedLatLonAltHozArray[0][0]
            val longitude = combinedLatLonAltHozArray[0][1]
            val altitude = combinedLatLonAltHozArray[0][2]
            val horizon = combinedLatLonAltHozArray[0][3] // distance to horizon
            //define end-points - end points are just points (lat,lon pairs) along the horizon circle spaced in ith angle increments
            val nOfRays = nOfRayHolder
            val coordsMasterList = mutableListOf<Array<Double>>()
            for (i in 0..nOfRays)
            {
                val bearing = (i*((Math.abs(endAngleHolder - startAngleHolder)/nOfRays)) + startAngleHolder)

                val currPair = EndPoint(bearing,horizon,latitude,longitude)

                coordsMasterList.add(i,currPair)
            }


            //now coordsMasterList stores all target points for our path that we will obtain from the url

            //we will now obtain nOfRays paths from the google api url
            val nOfSamples = nOfSamplesHolder // make this more efficient later
            val finalReturnArray = mutableListOf<Array<Double>>() // final array of coord pairs
            for (j in 0..nOfRays)
            {


                var altMasterList = mutableListOf<Array<Double>>()
                val targetLat = coordsMasterList[j][0]
                val targetLon = coordsMasterList[j][1]
                val currUrl = URL("https://maps.googleapis.com/maps/api/elevation/json?path=${latitude}%2C${longitude}%7C${targetLat}%2C${targetLon}&samples=${nOfSamples}&key=${server_api_key}")
                val connection = currUrl.openConnection()


                runOnUiThread() {
                    tempPolylineHolder = mMap?.addPolyline(PolylineOptions()
                        .clickable(false)
                        .add(
                            LatLng(latitude,longitude),LatLng(targetLat,targetLon)))!!
                }

                BufferedReader(InputStreamReader(connection.getInputStream())).use { inp ->
                    var line: String?
                    var eleHold = -99999.0
                    var latHold = 0.0
                    var lonHold = 0.0

                    //ok there may be a way to simplify this
                    var eleCheck = 0
                    var latCheck = 0
                    var lonCheck = 0



                    while (inp.readLine().also { line = it } != null) {


                        if (line?.contains("REQUEST DENIED") == true) {

                            eleHold = -99999.0
                            latHold = 0.0
                            lonHold = 0.0
                            val arrayOfReturns = arrayOf(eleHold,latHold,lonHold)
                            altMasterList.add(arrayOfReturns)

                        }
                        //alternatively if there is valid data
                        if (line?.contains("elevation") == true) {
                            eleHold = unpackURL(line,"elevation", 23)
                            eleCheck = 1

                        }
                        if (line?.contains("lat") == true) {
                            latHold = unpackURL(line,"lat", 20)
                            latCheck = 1

                        }
                        if (line?.contains("lng") == true) {
                            lonHold = unpackURL(line,"lng", 20)
                            lonCheck = 1

                        }
                        if ((eleCheck == 1 ) && (lonCheck == 1) && (latCheck == 1 ))
                        {
                            val arrayOfReturns = arrayOf(eleHold,latHold,lonHold)
                            altMasterList.add(arrayOfReturns)
                            eleCheck = 0
                            latCheck = 0
                            lonCheck = 0

                        }



                    }
                }
                runOnUiThread() {
                    tempPolylineHolder.remove()
                }

                val finalCoordinatePair = compareAltitudesForRay(altMasterList,altitude,targetLat, targetLon)
                finalReturnArray.add(finalCoordinatePair)

            }








            return finalReturnArray
        }



        //this function returns a pair of geo coordinates for a end point given a distance,angle and geo coordinates of a start point
        // (in this case the start point will always be the same: our location)
        private fun EndPoint(bearing: Double, distance: Double, lat1: Double, lon1: Double): Array<Double> {
            val R = 6371 // earths radius in km

            val lat1Rad = Math.toRadians(lat1)
            val lon1Rad = Math.toRadians(lon1)
            val berRad = Math.toRadians(bearing)

            var lat2 = Math.asin( Math.sin(lat1Rad)*Math.cos(distance/R) +
                    Math.cos(lat1Rad)*Math.sin(distance/R)*Math.cos(berRad) )


            var lon2 = lon1Rad + atan2(
                sin(berRad) * sin(distance / R) * cos(lat1Rad),
                cos(distance / R) - sin(lat1Rad) * sin(lat2)
            )

            lat2 = Math.toDegrees(lat2)
            lon2 = Math.toDegrees(lon2)


            return arrayOf(lat2, lon2)
        }
        private fun unpackURL(result : String?,dataType : String,concencationIndex : Int ) : Double
        {

            //todo: Replace elevationStr & finalElevation with more appropriate variable names

            //check if the arriving string is valid
            var finalElevation = 0.0 // if null returns 0.0 / if error returns -99999 / if correct returns correct altitude called from Google Maps
            var elevationStr = "0.0"



            if (result != null) {

                if (result.contains(dataType)) {

                    elevationStr = result.toString()
                    elevationStr = elevationStr.subSequence(concencationIndex,elevationStr.length) as String
                    elevationStr = elevationStr.subSequence(0,elevationStr.length - 1) as String


                    //TODO: WARNING THERES A BUG THAT CAUSES A FATAL ERROR IF THE COORDINATES ARE TOO PERFECT (EG. 0,0)
                    // ALSO IF ELEVATION IS NEGATIVE IT GETS FLIPPED SO WATCH OUT FOR THAT AND FIX IT LATER




                }

                finalElevation = elevationStr.toDouble()

            }
            return finalElevation
        }
        private fun compareAltitudesForRay(compiledAltiudeList : MutableList<Array<Double>>,ourAltitude : Double,targetLat : Double, targetLon : Double) : Array<Double> {
            //output the final coordinates for that point. If theres something higher blocking the horizon
            // then output that somethings coordinates. If not output the original curvature horizon coordinates

            //compiledAltitudeList = list for ONE ARRAY ONE
            var finalCoords = arrayOf(targetLat,targetLon)
            for (k in 0 .. compiledAltiudeList.size - 1) {

                if (compiledAltiudeList[k][0] < ourAltitude) {
                    finalCoords = arrayOf(compiledAltiudeList[k][1],compiledAltiudeList[k][2])


                }

            }

            return finalCoords
        }







    }



//}






