/*********************************************************************************************
 * Brainstorming for format:
 * <p/>
 * Fixing getResources:
 * http://stackoverflow.com/questions/32765906/android-getdrawable-deprecated-how-to-use-android-getdrawable
 * http://stackoverflow.com/questions/29041027/android-getresources-getdrawable-deprecated-api-22/34750353
 * http://developer.android.com/reference/android/support/v4/content/ContextCompat.html
 * /
 *********************************************************************************************/

package team8.personaltransportation;

// Sources:
// USB communication -
// http://developer.android.com/guide/topics/connectivity/usb/accessory.html
// http://www.java2s.com/Open-Source/Android_Free_Code/Example/code/com_examples_accessory_controllerMainUsbActivity_java.htm
// http://www.ftdichip.com/Support/SoftwareExamples/Android_Projects.htm

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import team8.personaltransportation.LIN.runtime.MasterDevice;
import team8.personaltransportation.LIN.runtime.Signal;
import team8.personaltransportation.LIN.runtime.SignalHeader;
import team8.personaltransportation.LIN.runtime.SignalReceivedListener;
import team8.personaltransportation.USB.MasterDeviceConnectionListener;
import team8.personaltransportation.USB.MasterManager;

public class FullscreenActivity extends Activity implements MasterDeviceConnectionListener, SignalReceivedListener {
    private ImageView GPSbutton;
    private TextView GPStextview;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private MasterManager masterManager;
    private MasterDevice master;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        // add sound to the button press
        final MediaPlayer pressButSound = MediaPlayer.create(FullscreenActivity.this, R.raw.robotblip);
        final MediaPlayer robot2 = MediaPlayer.create(FullscreenActivity.this, R.raw.robotblip2);
        final MediaPlayer pleasebut = MediaPlayer.create(FullscreenActivity.this, R.raw.pleaseturnoff);
        final MediaPlayer pindrop = MediaPlayer.create(FullscreenActivity.this, R.raw.pindrop);

        GPSbutton = (ImageView) findViewById(R.id.phoneconnbutn2);
        GPStextview = (TextView) findViewById(R.id.fullscreen_content);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                GPStextview.append("\n " + location.getLatitude() + " " + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                }, 10);
                return;
            }
        } else {
            configureButton();
        }

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mControlsView = findViewById(R.id.fullscreen_content_controls);


        /************************** Turn Signal *********************************************/
        final TwoStateButton rightTurnSignal = (TwoStateButton) this.findViewById(R.id.rightTurn);
        final TwoStateButton leftTurnSignal = (TwoStateButton) this.findViewById(R.id.leftTurn);
        final TwoStateButton hazardButton = (TwoStateButton) this.findViewById(R.id.warning);

        rightTurnSignal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hazardButton.isOn()) {
                    rightTurnSignal.toggle();
                    if (rightTurnSignal.isOn()) {
                        leftTurnSignal.setOn(false);
                    }
                    sendSignalLightState();
                }
            }
        });

        leftTurnSignal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hazardButton.isOn()) {
                    leftTurnSignal.toggle();
                    if (leftTurnSignal.isOn()) {
                        rightTurnSignal.setOn(false);
                    }
                    sendSignalLightState();
                }
            }
        });


        /************************** Hazard Button *********************************************/
        hazardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hazardButton.toggle();
                if (hazardButton.isOn()) {
                    // Turn them off first so the animations stay
                    // synchronized.
                    leftTurnSignal.setOn(false);
                    rightTurnSignal.setOn(false);
                    leftTurnSignal.setOn(true);
                    rightTurnSignal.setOn(true);
                } else {
                    leftTurnSignal.setOn(false);
                    rightTurnSignal.setOn(false);
                }
                sendSignalLightState();
            }
        });

        /************************** HEADLAMP **************************************/
        final TwoStateButton lowBeamButton = (TwoStateButton) findViewById(R.id.headLamp);
        final TwoStateButton highBeamButton = (TwoStateButton) findViewById(R.id.brights);

        lowBeamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressButSound.start();
                lowBeamButton.toggle();
                if (!lowBeamButton.isOn())
                    highBeamButton.setOn(false);
            }
        });

        highBeamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lowBeamButton.isOn()) {
                    pressButSound.start();
                    highBeamButton.toggle();
                } else {
                    highBeamButton.setOn(false);
                }
            }
        });

        /*************************** DEFROST ********************************************/
        final FourStateButton defrostButton = (FourStateButton) findViewById(R.id.defrost1);
        defrostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressButSound.start();
                defrostButton.nextState();
            }
        });


        /*************************** WIPERS ********************************************/
        final FourStateButton wiperButton = (FourStateButton) findViewById(R.id.wiper);
        wiperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressButSound.start();
                wiperButton.nextState();
            }
        });


        /*************************** BATTERY ********************************************/
        final BatteryButton batteryButton = (BatteryButton) findViewById(R.id.batteryLife);


        /*************************** SPEEDOMETER ******************************************/
        final SpeedButton leftSpeedButton = (SpeedButton) findViewById(R.id.leftspeedo);
        final SpeedButton rightSpeedButton = (SpeedButton) findViewById(R.id.rightspeedo);


        /************************** SETTINGS **************************************/
        final ImageView settingsButton = (ImageView) findViewById(R.id.settingsbutton);
        settingsButton.setImageResource(R.drawable.cirbuttonmsc);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pressButSound.start();
                //Toast toast3 = Toast.makeText(FullscreenActivity.this, "You Clicked Settings", Toast.LENGTH_LONG);
                //LinearLayout toastLayout = (LinearLayout) toast3.getView();
                //TextView toastTV = (TextView) toastLayout.getChildAt(0);
                //toast3.setGravity(Gravity.CENTER, 240, -500);
                //toastTV.setTextSize(30);
                //toast3.show();
                Intent i = new Intent(FullscreenActivity.this, ActivitySettings.class);
                startActivity(i);
            }
        });

        masterManager = new MasterManager();
        masterManager.onCreate(this);
        masterManager.addConnectionListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureButton();
                }
                return;
        }
    }

    private void configureButton() {
        GPSbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //locationManager.requestLocationUpdates("gps",5000,0,locationListener);
            }
        });
    }

// *********************************************************************************************************
// ******************End Cut Here******************************************************************************
// *********************************************************************************************************

    @Override
    public void onResume() {
        super.onResume();

        masterManager.onResume(getIntent());
    }


    @Override
    public void onPause() {
        super.onPause();

        masterManager.onPause();
    }


    @Override
    public void onDestroy() {

        masterManager.onDestroy(this);

        super.onDestroy();
    }

    @Override
    public void onMasterDeviceConnected(MasterDevice device) {
        Toast.makeText(this, "Master Connected", Toast.LENGTH_LONG).show();
        this.master = device;
        this.master.addSignalReceivedListener(this);
    }

    @Override
    public void onSignalReceived(Signal signal) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO fill this out.
            }
        });
    }

    @Override
    public void onMasterDeviceDisconnected(MasterDevice device) {
        if(this.master == device) {
            Toast.makeText(this, "Master Disconnected", Toast.LENGTH_LONG).show();
            this.master.removeSignalReceivedListener(this);
            this.master = null;
        }
    }

    public static int signalHash(byte [] input,int i) {
        return (input.length != i) ? ((int)input[i]) + 33 * signalHash(input,i+1) : 5381;
    }

    private void sendSignalLightState() {
        if(master != null) {
            TwoStateButton lt = (TwoStateButton) findViewById(R.id.leftTurn);
            TwoStateButton rt = (TwoStateButton) findViewById(R.id.rightTurn);
            TwoStateButton hz = (TwoStateButton) findViewById(R.id.warning);
            SignalHeader header = new SignalHeader();
            header.command = SignalHeader.COMMAND_SET;
            header.sid = signalHash("signal_light_state".getBytes(),0);
            header.length = 1;
            byte [] data = new byte[1];
            if(hz.isOn())
                data[0] = 3;
            else if(lt.isOn())
                data[0] = 2;
            else if(rt.isOn())
                data[0] = 1;
            else
                data[0] = 0;
            master.sendSignal(new Signal(header,data));
            Toast.makeText(FullscreenActivity.this,"is java really new",Toast.LENGTH_LONG).show();
        }
    }


// *********************************************************************************************************
// ******************End Cut Here******************************************************************************
// *********************************************************************************************************
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            Log.d("mShowPart2Runnable", "run...");
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }

        private ActionBar getSupportActionBar() {
            return null;
        }
    };

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.d("BUTTONS", "View.onTouchListener got here!");
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private ActionBar supportActionBar;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public ActionBar getSupportActionBar() {
        return supportActionBar;
    }
}

