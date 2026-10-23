package br.com.fiec.helptec.maps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.fiec.helptec.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMapsBinding

    private var latitudeAtual: Double = 0.0
    private var longitudeAtual: Double = 0.0
    private var localSelecionado = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        val concedida = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) {
            obterLocalizacaoGPS()
        } else {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtém o fragmento do mapa pelo ID definido no XML e carrega o mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Botão de voltar: devolve as coordenadas escolhidas para a tela anterior
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finalizarERetornarCoordenadas()
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        googleMap.uiSettings.isZoomControlsEnabled = true

        // Permite escolher um ponto tocando no mapa
        googleMap.setOnMapClickListener { latLng ->
            atualizarMarcador(latLng, "Local selecionado")
        }

        verificarPermissoesEObterLocalizacao()
    }

    private fun temPermissaoDeLocalizacao(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun verificarPermissoesEObterLocalizacao() {
        if (temPermissaoDeLocalizacao()) {
            obterLocalizacaoGPS()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private fun obterLocalizacaoGPS() {
        val mapa = mMap ?: return
        if (!temPermissaoDeLocalizacao()) return

        mapa.isMyLocationEnabled = true

        // getCurrentLocation pede uma posição nova (lastLocation costuma vir nula)
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val posicaoAtual = LatLng(location.latitude, location.longitude)
                    atualizarMarcador(posicaoAtual, "Sua posição")
                    mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicaoAtual, 15f))
                } else {
                    Toast.makeText(
                        this,
                        "Não foi possível obter a localização atual. Toque no mapa para escolher.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Erro ao obter a localização. Verifique se o GPS está ligado.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun atualizarMarcador(latLng: LatLng, titulo: String) {
        latitudeAtual = latLng.latitude
        longitudeAtual = latLng.longitude
        localSelecionado = true

        mMap?.apply {
            clear()
            addMarker(MarkerOptions().position(latLng).title(titulo))
        }
    }

    private fun finalizarERetornarCoordenadas() {
        if (localSelecionado) {
            val resultIntent = Intent().apply {
                putExtra("extra_latitude", latitudeAtual)
                putExtra("extra_longitude", longitudeAtual)
            }
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
}