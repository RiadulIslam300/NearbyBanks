package com.example.nearbybanks

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var map:MapboxMap
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(mapboxMap: MapboxMap) {
                map = mapboxMap
                mapboxMap.setStyle(Style.Builder().fromUri("https://map.barikoi.com/styles/barikoi-bangla/style.json?key={NDc4OTpDSEdMUFMwN0xD}"))

                if (isLocationPermissionGranted()) {
                    requestLocation()
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        })


    }


    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                 latitude = location.latitude
                 longitude = location.longitude
                val category = "bank"
                val apiKey = "NDc4OTpDSEdMUFMwN0xD"

                val url =
                    "https://barikoi.xyz/v2/api/search/nearby/category/$apiKey/1/10?longitude=$longitude&&latitude=$latitude&ptype=$category"

                FetchDataAsyncTask().execute(url)
            } ?: run {
                Toast.makeText(
                    applicationContext,
                    "Failed to retrieve location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    inner class FetchDataAsyncTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String): String {
            val url = URL(urls[0])
            val connection = url.openConnection() as HttpURLConnection
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            }
            return ""
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            if (result.isNullOrEmpty()) {
                Toast.makeText(applicationContext, "Result is empty", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObject = JSONObject(result)
                    if (jsonObject.has("places")) {
                        val banks = jsonObject.getJSONArray("places")
                        for (i in 0 until banks.length()) {
                            val bank = banks.getJSONObject(i)
                            val name = bank.getString("name")
                            val bankLatitude = bank.getDouble("latitude")
                            val bankLongitude = bank.getDouble("longitude")
                            val markerOptions = MarkerOptions()
                                .position(LatLng(bankLatitude, bankLongitude))
                                .title(name)

                            map.addMarker(markerOptions)
                        }
                    } else {
                        Toast.makeText(applicationContext, "No places found", Toast.LENGTH_SHORT).show()
                    }

                    val zoomLevel = 15.0
                    val userLatLng = LatLng(latitude, longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, zoomLevel))
                } catch (e: JSONException) {
                    Toast.makeText(applicationContext, "Error parsing JSON response", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }


        private fun parseResponse(response: String): JSONArray {
            val jsonObject = JSONObject(response)
            return jsonObject.getJSONArray("places")
        }
    }



    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


}