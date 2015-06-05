package com.playuav.android.activities;
import com.playuav.android.utils.DroneHelper;
import org.droidplanner.core.gcs.location.Location;
import org.droidplanner.core.gcs.location.Location.LocationFinder;
import org.droidplanner.core.gcs.location.Location.LocationReceiver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.MAVLink.common.msg_global_position_int;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.util.MathUtils;

import com.playuav.android.R;
import com.playuav.android.dialogs.openfile.OpenFileDialog;
import com.playuav.android.dialogs.openfile.OpenTLogDialog;
import com.playuav.android.fragments.LocatorListFragment;
import com.playuav.android.fragments.LocatorMapFragment;
import com.playuav.android.utils.file.IO.TLogReader;
import com.playuav.android.utils.prefs.AutoPanMode;
import org.droidplanner.core.helpers.coordinates.Coord2D;

import java.util.LinkedList;
import java.util.List;

/**
 * This implements the map locator activity. The map locator activity allows the user to find
 * a lost drone using last known GPS positions from the tlogs.
 */
public class LocatorActivity extends DrawerNavigationUI implements LocatorListFragment
        .OnLocatorListListener, LocationReceiver {

    private static final String STATE_LAST_SELECTED_POSITION = "STATE_LAST_SELECTED_POSITION";

    private final static List<msg_global_position_int> lastPositions = new
            LinkedList<msg_global_position_int>();

    /*
    View widgets.
     */
    private LocatorMapFragment locatorMapFragment;
    private LocatorListFragment locatorListFragment;
    private LinearLayout statusView;
    private TextView latView, lonView, distanceView, azimuthView;

    private msg_global_position_int selectedMsg;
    private LatLong lastGCSPosition;
    private float lastGCSBearingTo = Float.MAX_VALUE;
    private double lastGCSAzimuth = Double.MAX_VALUE;


    public List<msg_global_position_int> getLastPositions() {
        return lastPositions;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);

        FragmentManager fragmentManager = getSupportFragmentManager();

        locatorMapFragment = ((LocatorMapFragment) fragmentManager
                .findFragmentById(R.id.mapFragment));
        locatorListFragment = (LocatorListFragment) fragmentManager
                .findFragmentById(R.id.locatorListFragment);

        statusView = (LinearLayout) findViewById(R.id.statusView);
        latView = (TextView) findViewById(R.id.latView);
        lonView = (TextView) findViewById(R.id.lonView);
        distanceView = (TextView) findViewById(R.id.distanceView);
        azimuthView = (TextView) findViewById(R.id.azimuthView);

        final ImageButton resetMapBearing = (ImageButton) findViewById(R.id.map_orientation_button);
        resetMapBearing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locatorMapFragment != null) {
                    locatorMapFragment.updateMapBearing(0);
                }
            }
        });

        final ImageButton zoomToFit = (ImageButton) findViewById(R.id.zoom_to_fit_button);
        zoomToFit.setVisibility(View.VISIBLE);
        zoomToFit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locatorMapFragment != null){
                    locatorMapFragment.zoomToFit();
                }
            }
        });

        ImageButton mGoToMyLocation = (ImageButton) findViewById(R.id.my_location_button);
        mGoToMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locatorMapFragment.goToMyLocation();
            }
        });
        mGoToMyLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                locatorMapFragment.setAutoPanMode(AutoPanMode.USER);
                return true;
            }
        });

        ImageButton mGoToDroneLocation = (ImageButton) findViewById(R.id.drone_location_button);
        mGoToDroneLocation.setVisibility(View.GONE);

        // clear prev state if this is a fresh start
        if(savedInstanceState == null) {
            // fresh start
            lastPositions.clear();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        locatorMapFragment.setLocationReceiver(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        locatorMapFragment.setLocationReceiver(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final int lastSelectedPosition = lastPositions.indexOf(selectedMsg);
        outState.putInt(STATE_LAST_SELECTED_POSITION, lastSelectedPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final int lastSelectedPosition = savedInstanceState.getInt(STATE_LAST_SELECTED_POSITION, -1);
        if(lastSelectedPosition != -1 && lastSelectedPosition < lastPositions.size())
            setSelectedMsg(lastPositions.get(lastSelectedPosition));
    }

    @Override
    protected int getNavigationDrawerEntryId() {
        return R.id.navigation_locator;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_locator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_open_tlog_file:
                openLogFile();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openLogFile() {
        OpenFileDialog tlogDialog = new OpenTLogDialog() {
            @Override
            public void tlogFileLoaded(TLogReader reader) {
                loadLastPositions(reader.getLogEvents());
                locatorMapFragment.zoomToFit();
            }
        };
        tlogDialog.openDialog(this);
    }

    /*
    Copy all messages with non-zero coords -> lastPositions and reverse the list (most recent first)
     */
    private void loadLastPositions(List<TLogReader.Event> logEvents) {
        lastPositions.clear();

        for (TLogReader.Event event : logEvents) {
            final msg_global_position_int message = (msg_global_position_int) event.getMavLinkMessage();
            if(message.lat != 0 || message.lon != 0)
                lastPositions.add(0, message);
        }

        setSelectedMsg(null);
        locatorListFragment.notifyDataSetChanged();

        updateInfo();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateMapPadding();
    }

    private void updateMapPadding() {
        int bottomPadding = 0;

        if(lastPositions.size() > 0) {
            bottomPadding = locatorListFragment.getView().getHeight();
        }

        locatorMapFragment.setMapPadding(0, 0, 0, bottomPadding);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        locatorMapFragment.saveCameraPosition();
    }

    @Override
    public void onItemClick(msg_global_position_int msg) {
        setSelectedMsg(msg);

        locatorMapFragment.zoomToFit();
        updateInfo();
    }

    public void setSelectedMsg(msg_global_position_int msg) {
        selectedMsg = msg;

        final LatLong msgCoord;
        if(msg != null)
            msgCoord = coordFromMsgGlobalPositionInt(selectedMsg);
        else
            msgCoord = new LatLong(0, 0);
        locatorMapFragment.updateLastPosition(msgCoord);
    }

    private void updateInfo() {
        if(selectedMsg != null) {
            statusView.setVisibility(View.VISIBLE);

            // coords
            final LatLong msgCoord = coordFromMsgGlobalPositionInt(selectedMsg);

            // distance
            if(lastGCSPosition == null || lastGCSPosition.getLatitude() == 0 || lastGCSPosition
                    .getLongitude() == 0) {
                // unknown
                distanceView.setText(R.string.status_waiting_for_gps, TextView.BufferType.NORMAL);
                azimuthView.setText("");
            } else {
                String distance = String.format("Distance: %.01fm",
                        MathUtils.getDistance(lastGCSPosition, msgCoord));
                if(lastGCSBearingTo != Float.MAX_VALUE) {
                    final String bearing = String.format(" @ %.0f°", lastGCSBearingTo);
                    distance += bearing;
                }
                distanceView.setText(distance);

                if(lastGCSAzimuth != Double.MAX_VALUE) {
                    final String azimuth = String.format("Heading: %.0f°", lastGCSAzimuth);
                    azimuthView.setText(azimuth);
                }
            }

            latView.setText(String.format("Latitude: %f°", msgCoord.getLatitude()));
            lonView.setText(String.format("Longitude: %f°", msgCoord.getLongitude()));
        } else {
            statusView.setVisibility(View.INVISIBLE);
            latView.setText("");
            lonView.setText("");
            distanceView.setText("");
            azimuthView.setText("");
        }
    }

    private static LatLong coordFromMsgGlobalPositionInt(msg_global_position_int msg) {
        double lat = msg.lat;
        lat /= 1E7;

        double lon = msg.lon;
        lon /= 1E7;

        return new LatLong(lat, lon);
    }

    @Override
    public void onLocationChanged(Location location) {
        lastGCSPosition = new LatLong(location.getCoord().getLat(), location.getCoord().getLng());
        lastGCSAzimuth = location.getBearing();

        if(selectedMsg != null) {
            final LatLong msgCoord = coordFromMsgGlobalPositionInt(selectedMsg);
            float[] results = new float[3];
            computeDistanceAndBearing(location.getCoord().getLat(),location.getCoord().getLng(),msgCoord.getLatitude(),msgCoord.getLongitude(),results);
            lastGCSBearingTo = Math.round(results[1]);
            lastGCSBearingTo = (lastGCSBearingTo + 360) % 360;
        } else {
            lastGCSBearingTo = Float.MAX_VALUE;
        }

        updateInfo();
    }


    private static void computeDistanceAndBearing(double lat1, double lon1,
                                                  double lat2, double lon2, float[] results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                            (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                            (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                            (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                                    (B / 6.0) * cos2SM *
                                            (-3.0 + 4.0 * sinSigma * sinSigma) *
                                            (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                    (1.0 - C) * f * sinAlpha *
                            (sigma + C * sinSigma *
                                    (cos2SM + C * cosSigma *
                                            (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));
        results[0] = distance;
        if (results.length > 1) {
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                    cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results[1] = initialBearing;
            if (results.length > 2) {
                float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                        -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
                finalBearing *= 180.0 / Math.PI;
                results[2] = finalBearing;
            }
        }
    }



}