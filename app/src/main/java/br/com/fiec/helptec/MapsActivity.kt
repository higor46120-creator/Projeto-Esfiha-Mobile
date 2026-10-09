package br.com.fiec.helptec

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitudeAtual: Double = 0.0
    private var longitudeAtual: Double = 0.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            obterLocalizacaoGPS()
        } else {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Trata o clique no botão de voltar (Android 13+)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finalizarERetornarCoordenadas()
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Permite selecionar um ponto personalizado clicando na tela
        mMap.setOnMapClickListener { latLng ->
            atualizarMarcador(latLng, "Local Selecionado")
        }

        verificarPermissoesEObterLocalizacao()
    }

    private fun verificarPermissoesEObterLocalizacao() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            obterLocalizacaoGPS()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun obterLocalizacaoGPS() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val posicaoAtual = LatLng(location.latitude, location.longitude)
                    atualizarMarcador(posicaoAtual, "Sua Posição")
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicaoAtual, 15f))
                } else {
                    Toast.makeText(
                        this,
                        "Não foi possível obter a localização atual. Toque no mapa para escolher.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun atualizarMarcador(latLng: LatLng, titulo: String) {
        latitudeAtual = latLng.latitude
        longitudeAtual = latLng.longitude

        mMap.clear()
        mMap.addMarker(MarkerOptions().position(latLng).title(titulo))
    }

    private fun finalizarERetornarCoordenadas() {
        val resultIntent = Intent().apply {
            putExtra("extra_latitude", latitudeAtual)
            putExtra("extra_longitude", longitudeAtual)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}