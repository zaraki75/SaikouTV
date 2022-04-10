package ani.saikou.tv

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.TvLoginFragmentBinding
import ani.saikou.tv.utils.TVConnection
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener


class TVLoginFragment() : Fragment() {

    val connectionCallback = ConnectionCallback()
    lateinit var binding: TvLoginFragmentBinding

    val singlePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> {
                startDiscovery()
            }
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                binding.text.text = "Permission denied"
            }
            else -> {
                binding.text.text = "Permission denied"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TvLoginFragmentBinding.inflate(inflater)
        binding.text.text = "Login Fragment"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singlePermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        Nearby.getConnectionsClient(requireContext())
            .startDiscovery(TVConnection.SERVICE_ID, object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
                    if(p1.serviceId == TVConnection.SERVICE_ID && p1.endpointName == TVConnection.PHONE_NAME)
                    Nearby.getConnectionsClient(context!!)
                        .requestConnection(
                            TVConnection.TV_NAME,
                            p0,
                            connectionCallback
                        )
                        .addOnSuccessListener(
                            OnSuccessListener { unused: Void? -> })
                        .addOnFailureListener(
                            OnFailureListener { e: Exception? ->
                                binding.text.text = "Something went wrong"
                            })

                }

                override fun onEndpointLost(p0: String) {

                }
            }, discoveryOptions)
            .addOnSuccessListener {
                binding.text.text = "Please open Saikou on your phone and login"
            }
            .addOnFailureListener { e: java.lang.Exception? ->
                binding.text.text = e?.message ?: "Error"
            }
    }

    inner class ConnectionCallback: ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            binding.text.text = p1.authenticationDigits
            Nearby.getConnectionsClient(requireContext())
                .acceptConnection(p0, object : PayloadCallback() {
                    override fun onPayloadReceived(p0: String, p1: Payload) {
                        if (p1.getType() === Payload.Type.BYTES) {
                            p1?.asBytes()?.let {
                                val token = String(it)
                                saveToken(token)
                                Nearby.getConnectionsClient(requireContext()).disconnectFromEndpoint(p0)
                                requireActivity().supportFragmentManager.popBackStack()
                            } ?: run {
                                binding.text.text = "Something is wrong with the token"
                            }
                        }
                    }
                    override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {}
                })
        }

        override fun onConnectionResult(
            p0: String,
            p1: ConnectionResolution
        ) {
            when (p1.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Nearby.getConnectionsClient(requireContext()).stopAdvertising()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                }
                else -> {
                }
            }
        }

        override fun onDisconnected(p0: String) {}
    }

    private fun saveToken(token: String) {
        Anilist.token = token
        val filename = "anilistToken"
        requireActivity().openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(token.toByteArray())
        }
    }
}