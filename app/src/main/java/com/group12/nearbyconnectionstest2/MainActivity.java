package com.group12.nearbyconnectionstest2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button discoverButton, advertiseButton;
    ListView peerListView;
    ArrayList<String> deviceNameArray;
    ArrayAdapter<String> adapter;
    TextView deviceNameTextView;
    ProgressBar progressBar;
    ConnectionsClient connectionsClient;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private static final String SERVICE_ID = "Journey Sharing";
    // Our randomly generated name
    private final String codeName = CodenameGenerator.generate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionsClient = Nearby.getConnectionsClient(this);

        discoverButton = findViewById(R.id.discoverButton);
        advertiseButton = findViewById(R.id.advertiseButton);
        peerListView= findViewById(R.id.peerListView);
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);
        discoverButton.setVisibility(View.GONE);

        deviceNameTextView.setText(codeName);

        deviceNameArray = new ArrayList<>();
        adapter= new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
        peerListView.setAdapter(adapter);

        advertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "advertise button pressed", Toast.LENGTH_SHORT).show();
                startAdvertising();
                startDiscovery();
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "discover button pressed", Toast.LENGTH_SHORT).show();
//                startAdvertising();
//                startDiscovery();
            }
        });

        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Nearby.getConnectionsClient(MainActivity.this)
                        .requestConnection(codeName, deviceNameArray.get(position), connectionLifecycleCallback)
                        .addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(MainActivity.this, "Connection Requested", Toast.LENGTH_SHORT).show();
                                        // We successfully requested a connection. Now both sides
                                           // must accept before the connection is established.
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Nearby Connections failed to request the connection.
                                        Toast.makeText(MainActivity.this, "Failed to request connection", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                        stopNearbyConnections();
                                    }
                                });
            }
        });
    }

    private void startAdvertising() {
        Toast.makeText(this, "In startAdvertising()", Toast.LENGTH_SHORT).show();
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(codeName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    // We're advertising!
                    Toast.makeText(MainActivity.this, "Advertising successful", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("ADVERTISING FAILURE: ", e.getMessage());
                        // We were unable to start advertising.
                        stopNearbyConnections();
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Unable to start advertising", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void startDiscovery() {
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    // We're discovering!
                    Toast.makeText(MainActivity.this, "Discovery started successfully", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // We're unable to start discovering.
                        Log.d("DISCOVERY FAILURE: ", e.getMessage());
                        Toast.makeText(MainActivity.this, "Unable to start discovery", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        stopNearbyConnections();
                    }
                });
    }


    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull final DiscoveredEndpointInfo info) {
            // An endpoint was found. We request a connection to it.

            deviceNameArray.add(info.getEndpointName());
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
//            Nearby.getConnectionsClient(MainActivity.this)
//                .requestConnection(codeName, endpointId, connectionLifecycleCallback)
//                .addOnSuccessListener(
//                        new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void unused) {
//                                Toast.makeText(MainActivity.this, "Connection Requested", Toast.LENGTH_SHORT).show();
//                                // We successfully requested a connection. Now both sides
//                                // must accept before the connection is established.
//                            }
//                        })
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // Nearby Connections failed to request the connection.
//                                Toast.makeText(MainActivity.this, "Failed to request connection", Toast.LENGTH_SHORT).show();
//                            }
//                        });
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            // A previously discovered endpoint has gone away.
            Toast.makeText(MainActivity.this, endpointId + " lost", Toast.LENGTH_SHORT).show();
            stopNearbyConnections();
            progressBar.setVisibility(View.GONE);
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull final String endpointId, ConnectionInfo connectionInfo) {
            // Automatically accept the connection on both sides.


            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                    .setMessage("Confirm the code matches on both devices: " + connectionInfo.getAuthenticationToken())
                    .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectionsClient.acceptConnection(endpointId, payloadCallback);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectionsClient.rejectConnection(endpointId);
                        }
                    })
                    .show();
            Toast.makeText(MainActivity.this, "Connection initiated", Toast.LENGTH_SHORT).show();
//                    connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {

            progressBar.setVisibility(View.GONE);

            stopNearbyConnections();
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    // We're connected! Can now start sending and receiving data.
                    Payload bytesPayload = Payload.fromBytes(new byte[] {0xa, 0xb, 0xc, 0xd});
                    connectionsClient.sendPayload(endpointId, bytesPayload);

                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Toast.makeText(MainActivity.this, "Connection Rejected", Toast.LENGTH_SHORT).show();
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    // Unknown status code
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            stopNearbyConnections();
            progressBar.setVisibility(View.GONE);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            Toast.makeText(MainActivity.this, "Payload Received", Toast.LENGTH_SHORT).show();
            byte[] receivedBytes = payload.asBytes();
            if (receivedBytes != null) {
                Toast.makeText(MainActivity.this, Arrays.toString(receivedBytes), Toast.LENGTH_SHORT).show();
            }
            stopNearbyConnections();
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    public void stopNearbyConnections()
    {
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
        connectionsClient.stopAllEndpoints();

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }
    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();

        super.onStop();
    }

}
