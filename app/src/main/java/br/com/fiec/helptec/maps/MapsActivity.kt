package br.com.fiec.helptec.maps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.R

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import br.com.fiec.helptec.data.ApiService
import br.com.fiec.helptec.databinding.ActivityMainBinding
import br.com.fiec.helptec.databinding.ActivityMapsBinding
import br.com.fiec.helptec.main.MainViewModel
import br.com.fiec.helptec.network.RetrofitClient
import br.com.fiec.helptec.network.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitudeAtual: Double = 0.0
    private var longitudeAtual: Double = 0.0

    private lateinit var activityMapsBinding: ActivityMapsBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            obterLocalizacaoGPS()
        } else {
            Toast.makeText(this, "PermissÃ£o de localizaÃ§Ã£o negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMapsBinding = ActivityMapsBinding.inflate(layoutInflater)

        setContentView(activityMapsBinding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // 3. Obtém oSupportMapFragment diretamente do binding pelo ID definido no XML
        // 3. Obtém o SupportMapFragment diretamente usando o ID do binding
        val mapFragment = supportFragmentManager
            .findFragmentById(activityMapsBinding.map.id) as SupportMapFragment

// 4. Carrega o mapa assincronamente
        mapFragment.getMapAsync(this)


        // Trata o clique no botÃ£o de voltar (Android 13+)
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
                    atualizarMarcador(posicaoAtual, "Sua PosiÃ§Ã£o")
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicaoAtual, 15f))
                } else {
                    Toast.makeText(
                        this,
                        "NÃ£o foi possÃ­vel obter a localizaÃ§Ã£o atual. Toque no mapa para escolher.",
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

private fun OnBackPressedDispatcher.addCallback(
    owner: MapsActivity,
    onBackPressedCallback: Any
) {
}
