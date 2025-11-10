package com.example.marketplaceapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.example.marketplaceapp.databinding.ActivitySeleccionarUbicacionBinding

class SeleccionarUbicacion : AppCompatActivity() , OnMapReadyCallback {

    private lateinit var binding : ActivitySeleccionarUbicacionBinding

    private companion object{
        private const val DEFAULT_ZOOM = 15
        private const val TAG = "SeleccionarUbicacion"
    }

    private var mMap : GoogleMap?=null

    private var mPlaceClient : PlacesClient?=null
    private var mFusedLocationProviderClient : FusedLocationProviderClient?=null

    private var mLastKnownLocation : Location?=null
    private var selectedLatitude : Double?=null
    private var selectedLongitude : Double?=null
    private var direccion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeleccionarUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listoLl.visibility = View.GONE

        val mapFragment = supportFragmentManager.findFragmentById(R.id.MapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        try {
            if (!Places.isInitialized()) {
                Places.initialize(this, getString(R.string.mi_google_maps_api_key))
            }
            mPlaceClient = Places.createClient(this)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al inicializar Places: ${e.message}", e)
            Toast.makeText(this, 
                "Error al inicializar el mapa. Verifica tu API Key de Google Maps", 
                Toast.LENGTH_LONG).show()
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            val autocompleteSupportMapFragment = supportFragmentManager.findFragmentById(R.id.autocompletar_fragment)
                    as AutocompleteSupportFragment

            val placeList = arrayOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG)

            autocompleteSupportMapFragment.setPlaceFields(listOf(*placeList))

            autocompleteSupportMapFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener{
                override fun onPlaceSelected(place: Place) {
                    val id = place.id
                    val name = place.name
                    val latlng = place.latLng

                    selectedLatitude = latlng?.latitude
                    selectedLongitude = latlng?.longitude
                    direccion = place.address?: ""

                    agregarMarcador(latlng, name, direccion)
                }
                override fun onError(p0: Status) {
                    android.util.Log.e(TAG, "Error en búsqueda: ${p0.statusMessage}")
                    Toast.makeText(applicationContext,"Error en búsqueda: ${p0.statusMessage}",Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al configurar autocomplete: ${e.message}", e)
        }

        binding.IbGsp.setOnClickListener {
            if (esGpsActivado()){
                solicitarPermisoLocacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }else{
                Toast.makeText(this,"!La ubicación no está activada¡. Actívalo para mostrar la ubicación actual",Toast.LENGTH_SHORT).show()
            }
        }

        binding.BtnListo.setOnClickListener {
            val intent = Intent()
            intent.putExtra("latitud",selectedLatitude)
            intent.putExtra("longitud",selectedLongitude)
            intent.putExtra("direccion",direccion)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun elegirLugarActual(){
        if (mMap == null){
            return
        }
        detectAndShowDeviceLocationMap()
    }

    @SuppressLint("MissingPermission")
    private fun detectAndShowDeviceLocationMap(){
        try {
            val locationResult = mFusedLocationProviderClient!!.lastLocation
            locationResult.addOnSuccessListener { location->
                if (location!=null){
                    mLastKnownLocation = location

                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude

                    val latLng = LatLng(selectedLatitude!!, selectedLongitude!!)
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
                    mMap!!.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM.toFloat()))

                    direccionLatLng(latLng)
                } else {
                    android.util.Log.w(TAG, "Location es null")
                    Toast.makeText(this, 
                        "No se pudo obtener la ubicación actual", 
                        Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener { e->
                android.util.Log.e(TAG, "Error al obtener ubicación: ${e.message}", e)
                Toast.makeText(this, 
                    "Error al obtener ubicación: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }catch (e:Exception){
            android.util.Log.e(TAG, "Exception en detectAndShowDeviceLocationMap: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (mMap != null) {
            solicitarPermisoLocacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            mMap!!.setOnMapClickListener {latlng->
                selectedLatitude = latlng.latitude
                selectedLongitude = latlng.longitude

                direccionLatLng(latlng)
            }
        } else {
            android.util.Log.e(TAG, "mMap es null en onMapReady")
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun direccionLatLng(latlng: LatLng) {
        val geoCoder = Geocoder(this)
        try {
            val addressList = geoCoder.getFromLocation(latlng.latitude, latlng.longitude,1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val addressLine = address.getAddressLine(0)
                val subLocality = address.subLocality
                direccion= "$addressLine"
                agregarMarcador(latlng,"$subLocality","$addressLine")
            } else {
                Toast.makeText(this, "No se pudo obtener la dirección", Toast.LENGTH_SHORT).show()
            }
        }catch (e:Exception){
            android.util.Log.e(TAG, "Error en direccionLatLng: ${e.message}", e)
            Toast.makeText(this, "Error al obtener dirección: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun agregarMarcador(latlng: LatLng?, titulo : String, direccion : String){
        if (latlng == null) {
            Toast.makeText(this, "Ubicación inválida", Toast.LENGTH_SHORT).show()
            return
        }
        
        mMap!!.clear()
        try {
            val markerOptions = MarkerOptions()
            markerOptions.position(latlng)
            markerOptions.title("$titulo")
            markerOptions.snippet("$direccion")
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

            mMap!!.addMarker(markerOptions)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, DEFAULT_ZOOM.toFloat()))

            binding.listoLl.visibility = View.VISIBLE
            binding.lugarSelecTv.text = direccion
        }catch (e:Exception){
            android.util.Log.e(TAG, "Error al agregar marcador: ${e.message}", e)
            Toast.makeText(this, "Error al agregar marcador: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun esGpsActivado(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnable = false
        var networkEnable = false
        try {
            gpsEnable = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }catch (e:Exception){

        }
        try {
            networkEnable = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }catch (e:Exception){

        }

        return !(!gpsEnable && !networkEnable)

    }

    @SuppressLint("MissingPermission")
    private val solicitarPermisoLocacion : ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){seConcede->
            if (seConcede){
                mMap!!.isMyLocationEnabled = true
                elegirLugarActual()
            }else{
                Toast.makeText(this,"Permiso denegado",Toast.LENGTH_SHORT).show()
            }
        }

}
