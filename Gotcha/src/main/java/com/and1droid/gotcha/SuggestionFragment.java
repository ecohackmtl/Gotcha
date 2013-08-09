package com.and1droid.gotcha;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestionFragment extends Fragment {

    public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;

    private static final String EXTRA_IMAGE_URI = "EXTRA_IMAGE_URI";
    private static final String EXTRA_NAME = "EXTRA_NAME";
    private static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    private static final String EXTRA_LATLONG = "EXTRA_LATLONG";

    protected Uri fileUri;

    private LocationClient locationClient;
    private boolean pictureTaken = false;
    private Geocoder geocoder;
    private Button suggestionButton;
    private List<Address> addresses = new ArrayList<Address>();
    private TextView textViewAddress;
    private double[] latLong = new double[2];

    private String[] adresse = new String[3];

    private EditText editTextName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (ConnectionResult.SUCCESS == googlePlayServicesAvailable) {
            locationClient = new LocationClient(getActivity(), connectionCallbacks, onConnectionFailedListener);
            locationClient.connect();
        }
        geocoder = new Geocoder(getActivity(), Locale.getDefault());
    }

    public static Fragment newFragment() {
        return new SuggestionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.suggestion_fragment, container, false);
        suggestionButton = (Button) view.findViewById(R.id.suggestButton);
        suggestionButton.setEnabled(false);
        suggestionButton.setOnClickListener(onClickListener);
        textViewAddress = (TextView) view.findViewById(R.id.textViewAddress);
        editTextName = (EditText) view.findViewById(R.id.editTextName);
        editTextName.addTextChangedListener(watcher);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (locationClient.isConnected()) {
            Location lastLocation = locationClient.getLastLocation();
            if (lastLocation != null) {
                latLong[0] = lastLocation.getLatitude();
                latLong[1] = lastLocation.getLongitude();
                try {
                    addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1);
                    Address address = addresses.get(0);
                    adresse[0] = address.getAddressLine(0);
                    adresse[1] = address.getAddressLine(1);
                    adresse[2] = address.getAddressLine(2);
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), e.getMessage(), e);
                }
                textViewAddress.setText(adresse[0] + " " + adresse[1] + " " + adresse[2]);
                suggestionButton.setEnabled(isSuggestionButtonEnabled(lastLocation));
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!pictureTaken) {
            pictureTaken = true;
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FindAPlayground");
            fileUri = Uri.fromFile(f);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    private boolean isSuggestionButtonEnabled(Location lastLocation) {
        return lastLocation != null && editTextName.getText().length() > 3;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SuggestionFragment.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                ImageView imageView = (ImageView) getView().findViewById(R.id.imageView1);
                new BitmapWorkerTask(imageView).execute(fileUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private OnClickListener onClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.suggestButton:
                    Intent intent = new Intent().putExtra(EXTRA_IMAGE_URI, fileUri.getPath()).putExtra(EXTRA_NAME, editTextName.getText().toString()).putExtra(EXTRA_LATLONG, latLong)
                            .putExtra(EXTRA_ADDRESS, adresse);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                default:
                    break;
            }
        }
    };

    private GooglePlayServicesClient.ConnectionCallbacks connectionCallbacks = new GooglePlayServicesClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.i(getClass().getSimpleName(), "Connected at GooglePlayService");
            LocationRequest request = LocationRequest.create();
            request.setInterval(60000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            Log.i(getClass().getSimpleName(), "Trying to locate user");
            locationClient.requestLocationUpdates(request, onLocationChangeListener);
        }

        @Override
        public void onDisconnected() {
            Log.i(getClass().getSimpleName(), "Disconnected at GooglePlayService");
        }
    };

    private GooglePlayServicesClient.OnConnectionFailedListener onConnectionFailedListener = new GooglePlayServicesClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.i(getClass().getSimpleName(), "Connection failed at GooglePlayService");
        }
    };

    private LocationListener onLocationChangeListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(getClass().getSimpleName(), "User Located lat " + location.getLatitude() + " lng " + location.getLongitude());
        }
    };

    class BitmapWorkerTask extends AsyncTask<Uri, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Uri... params) {
            fileUri = params[0];
            return BitmapUtil.getResizedBitmap(fileUri);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

    }

    private TextWatcher watcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable editable) {
            if (!suggestionButton.isEnabled()) {
                if (locationClient.isConnected()) {
                    suggestionButton.setEnabled(isSuggestionButtonEnabled(locationClient.getLastLocation()));
                }

            }
        }

        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

    };
}
