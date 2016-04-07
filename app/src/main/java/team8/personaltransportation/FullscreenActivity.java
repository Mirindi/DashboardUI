package team8.personaltransportation;

// Sources:
// USB communication -
// http://developer.android.com/guide/topics/connectivity/usb/accessory.html
// http://www.java2s.com/Open-Source/Android_Free_Code/Example/code/com_examples_accessory_controllerMainUsbActivity_java.htm
// http://www.ftdichip.com/Support/SoftwareExamples/Android_Projects.htm

import android.Manifest;
import android.annotation.SuppressLint;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.app.AppCompactActivity;
//import android.app.ActivityManager;
//import android.app.ActivityOptions;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

//public class FullscreenActivity extends AppCompatActivity {
public class FullscreenActivity extends Activity {


    private int SID_BATTERY; // TODO fill this out with hash
    private int SID_LIGHTS; // TODO fill this out with hash
    private int SID_SPEED; // TODO fill this out with hash


    // variables for GUI interface
    int batteryLife = 0;
    boolean warningOn = false;
    boolean headlampOn = false;
    boolean brightsOn = false;
    int wiperswitch = 0;
    int defrostswitch = 0;
    private ImageView GPSbutton;
    private TextView GPStextview;
    private LocationManager locationManager;
    private LocationListener locationListener;

    USB_Send_Receive usb_send_receive;

    // TEST _ JOSEPH
    ImageView batButton;
    int batButtonSwitch = 0;

    Handler usbInputHandler;
    LinBus linBus;

    AnimationDrawable rightAnim;
    AnimationDrawable leftAnim;
    AnimationDrawable hazardAnim;
    int rightduration = 150;
    int leftduration = 150;
    int hazarduration = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        // add sound to the button press
        final MediaPlayer pressButSound = MediaPlayer.create(FullscreenActivity.this, R.raw.horn);
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
                       Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.INTERNET
                }, 10);
                return;
            }
        }
        else {
            configureButton();
        }

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        //final ImageView warningButton = (ImageView) findViewById(R.id.warning);
        /*** Ming's ***/
        final ImageView rightturn = (ImageView) this.findViewById(R.id.rightTurn);
        final ImageView leftturn = (ImageView) this.findViewById(R.id.leftTurn);
        final ImageView hazardbut = (ImageView) this.findViewById(R.id.warning);

        hazardAnim = new AnimationDrawable();
        hazardAnim.addFrame(getResources().getDrawable(R.drawable.warningoffnew), hazarduration);
        hazardAnim.addFrame(getResources().getDrawable(R.drawable.warningonbnew), hazarduration);
        hazardAnim.addFrame(getResources().getDrawable(R.drawable.warningonnew), hazarduration);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            hazardbut.setBackgroundDrawable(hazardAnim);
        } else {
            hazardbut.setBackground(hazardAnim);
        }

        hazardbut.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub

                pressButSound.start();
                Toast toast2 = Toast.makeText(FullscreenActivity.this, "Hazards On, Contacting Emergency Services.", Toast.LENGTH_SHORT);
                LinearLayout toastLayout = (LinearLayout) toast2.getView();
                TextView toastTV = (TextView) toastLayout.getChildAt(0);
                toast2.setGravity(Gravity.CENTER, 240, -500);
                toastTV.setTextSize(30);
                toast2.show();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        if (hazardAnim.isRunning()) {
                            hazardAnim.stop();
                            hazardAnim.addFrame(getResources().getDrawable(R.drawable.warningoffnew), hazarduration);
                            return true;
                        } else {
                            hazardAnim.setOneShot(false);
                            hazardAnim.start();
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        //warningButton.setImageResource(R.drawable.warningoffnew);
       /* warningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("BUTTON", "I clicked it!");

                if (warningOn) {
                    pressButSound.start();
                    Toast toast1 = Toast.makeText(FullscreenActivity.this, "Hazards Off", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast1.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast1.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast1.show();
                    warningButton.setImageResource(R.drawable.warningoffnew);
                    warningOn = false;
                } else {
                    pressButSound.start();
                    Toast toast2 = Toast.makeText(FullscreenActivity.this, "Hazards On, Contacting Emergency Services.", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast2.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast2.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast2.show();
                    warningButton.setImageResource(R.drawable.warningonnew);
                    warningOn = true;
                }
            }
        });*/

        rightAnim = new AnimationDrawable();
        rightAnim.addFrame(getResources().getDrawable(R.drawable.rightturnsignaloffnew), rightduration);
        rightAnim.addFrame(getResources().getDrawable(R.drawable.rightturnsignal1new), rightduration);
        rightAnim.addFrame(getResources().getDrawable(R.drawable.rightturnsignal2new), rightduration);
        rightAnim.addFrame(getResources().getDrawable(R.drawable.rightturnsignal3new), rightduration);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            rightturn.setBackgroundDrawable(rightAnim);
        } else {
            rightturn.setBackground(rightAnim);
        }

        rightturn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {

                        if (rightAnim.isRunning()) {
                            rightAnim.stop();
                            rightAnim.addFrame(getResources().getDrawable(R.drawable.rightturnsignaloffnew), rightduration);
                            return true;
                        } else {
                            rightAnim.setOneShot(false);
                            rightAnim.start();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        leftAnim = new AnimationDrawable();
        leftAnim.addFrame(getResources().getDrawable(R.drawable.leftturnsignaloffnew), leftduration);
        leftAnim.addFrame(getResources().getDrawable(R.drawable.leftturnsignal1new), leftduration);
        leftAnim.addFrame(getResources().getDrawable(R.drawable.leftturnsignal2new), leftduration);
        leftAnim.addFrame(getResources().getDrawable(R.drawable.leftturnsignal3new), leftduration);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            leftturn.setBackgroundDrawable(leftAnim);
        } else {
            leftturn.setBackground(leftAnim);
        }

        leftturn.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {

                        if (leftAnim.isRunning()) {
                            leftAnim.stop();
                            leftAnim.addFrame(getResources().getDrawable(R.drawable.leftturnsignaloffnew), leftduration);
                            return true;
                        } else {
                            leftAnim.setOneShot(false);
                            leftAnim.start();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();



        final ImageView settingsButton = (ImageView) findViewById(R.id.settingsbutton);
        settingsButton.setImageResource(R.drawable.cirbuttonmsc);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pressButSound.start();
                Toast toast3 = Toast.makeText(FullscreenActivity.this, "You Clicked Settings", Toast.LENGTH_LONG);
                LinearLayout toastLayout = (LinearLayout) toast3.getView();
                TextView toastTV = (TextView) toastLayout.getChildAt(0);
                toast3.setGravity(Gravity.CENTER, 240, -500);
                toastTV.setTextSize(30);
                toast3.show();
                Intent i = new Intent(FullscreenActivity.this, activitysettings.class);
                startActivity(i);
                //Intent intent = new Intent();
                //intent.setAction(Intent.ACTION_VIEW);
                //startActivity(new Intent(FullscreenActivity.this, activitysettings.class));
            }
        });

        final ImageView headlampButton = (ImageView) findViewById(R.id.headLamp);
        headlampButton.setImageResource(R.drawable.headlampoffnew);
        headlampButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("BUTTON", "I clicked it!");

                if (headlampOn) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Headlamps Off", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    headlampButton.setImageResource(R.drawable.headlampoffnew);
                    headlampOn = false;
                } else {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Headlamps On", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    headlampButton.setImageResource(R.drawable.headlamponnew);
                    headlampOn = true;
                }
            }
        });

        final ImageView brightsButton = (ImageView) findViewById(R.id.brights);
        //headlampButton.setImageResource(R.drawable.brightsoffnew);
        brightsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("BUTTON", "I clicked it!");

                if (brightsOn) {
                    pressButSound.start();
                    Toast toast =  Toast.makeText(FullscreenActivity.this, "Brights Off", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    brightsButton.setImageResource(R.drawable.brightsoffnew);
                    brightsOn = false;
                } else {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Brights On", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    brightsButton.setImageResource(R.drawable.brightsonnew);
                    brightsOn = true;
                }
            }
        });


        /*************************** working on this ********************************************/
        final ImageView defrostButton = (ImageView) findViewById(R.id.defrost1);
        //defrostButton.setImageResource(R.drawable.defrostoffc);
        defrostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("BUTTON", "I clicked it!");

                if (defrostswitch == 0) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Defrost Low", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    defrostButton.setImageResource(R.drawable.defroston1new);
    defrostswitch = 1;
                } else if (defrostswitch == 1) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Defrost Medium", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    defrostButton.setImageResource(R.drawable.defroston2new);
                    defrostswitch = 2;
                } else if (defrostswitch == 2) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Defrost High", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    defrostButton.setImageResource(R.drawable.defroston3new);
                    defrostswitch = 3;
                } else {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Defrost Off", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    defrostButton.setImageResource(R.drawable.defrostoffnew);
                    defrostswitch = 0;
                }
            }
        });

        final ImageView batteryButton = (ImageView) findViewById(R.id.batteryLife);
        batteryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(FullscreenActivity.this, "Battery Level is at " + batteryLife + "%", Toast.LENGTH_LONG);
                LinearLayout toastLayout = (LinearLayout) toast.getView();
                TextView toastTV = (TextView) toastLayout.getChildAt(0);
                toast.setGravity(Gravity.CENTER, 200, -500);
                toastTV.setTextSize(30);
                toast.show();
            }
        });
        /*************************** working ********************************************/
        final ImageView wiperButton = (ImageView) findViewById(R.id.wiper);
        //wiperButton.setImageResource(R.drawable.wipersoffnew);
        wiperButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Log.d("BUTTON", "I clicked it!");

                if (wiperswitch == 0) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Wipers Low", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    wiperButton.setImageResource(R.drawable.wiperson1new);
                    wiperswitch = 1;
                } else if (wiperswitch == 1) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Wipers Medium", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    wiperButton.setImageResource(R.drawable.wiperson2new);
                    wiperswitch = 2;
                } else if (wiperswitch == 2) {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Wipers High", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    wiperButton.setImageResource(R.drawable.wiperson3new);
                    wiperswitch = 3;
                } else {
                    pressButSound.start();
                    Toast toast = Toast.makeText(FullscreenActivity.this, "Wipers Off", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toast.setGravity(Gravity.CENTER, 240, -500);
                    toastTV.setTextSize(30);
                    toast.show();
                    wiperButton.setImageResource(R.drawable.wipersoffnew);
                    wiperswitch = 0;
                }
            }
        });


        /*************************** working ********************************************/
        batButton = (ImageView) findViewById(R.id.batteryLife);
        //batButton.setImageResource(R.drawable.battery100);        //switched this to a speed bezel for another view setup


        // Handles incoming messages
        usbInputHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                // XXX perform functionality to handle message (and provide response to USB with UIoutputStream.write())
                // XXX Temporary: spit back input data in message to USB

                Log.d("UIHandler", "handling message: " + msg);


                //final ImageButton warningButton = (ImageButton) activity.findViewById(R.id.warning);
                //warningButton.setImageResource(R.drawable.warningon); // Example that images can be set in these handlers


                LinSignal signal = (LinSignal) msg.obj;

                Toast.makeText(getApplicationContext(), String.valueOf(LinSignal.signalHash("BATTERY".getBytes(), 0)) + ", " + String.valueOf(signal.sid), Toast.LENGTH_LONG).show();

                if (LinSignal.signalHash("BATTERY".getBytes(), 0) == signal.sid) {

                    if (signal.command == LinSignal.COMM_SET_VAR) {
                        // TODO: setting battery is not finished
                        String Data_IN_TEST = signal.data.toString();
                        //String Data_String_TEST = Arrays.copyOfRange(FullscreenActivity.linSignal.data, 0, 2).toString();
                        Toast.makeText(getApplicationContext(), "TEST - CHANGED Battery::", Toast.LENGTH_SHORT).show();

                        batteryLife = LinSignal.unpackBytesToInt(signal.data[0], signal.data[1], signal.data[2], signal.data[3]);

                        if (batteryLife < 5) {
                            batButton.setImageResource(R.drawable.battery00new);
                        } else if (batteryLife <= 20) {
                            batButton.setImageResource(R.drawable.battery20new);
                        } else if (batteryLife <= 40) {
                            batButton.setImageResource(R.drawable.battery40new);
                        } else if (batteryLife <= 60) {
                            batButton.setImageResource(R.drawable.battery60new);
                        } else if (batteryLife <= 80) {
                            batButton.setImageResource(R.drawable.battery80new);
                        } else {
                            batButton.setImageResource(R.drawable.battery100new);
                        }

                        //LinSignal sendSig = new LinSignal(LinSignal.COMM_SET_VAR, signal.sid, (byte) 4, LinSignal.packIntToBytes(batteryLife));
                        //linBus.sendSignal(sendSig);
                        linBus.sendSignal(signal);
                    } else if (signal.command == LinSignal.COMM_WARN_VAR) {
                        // TODO
                    }

                } else if (LinSignal.signalHash("SPEED".getBytes(), 0) == signal.sid) {
                    if (signal.command == LinSignal.COMM_SET_VAR) {
                        // TODO
                    } else if (signal.command == LinSignal.COMM_WARN_VAR) {
                        // TODO
                    }

                } else if (LinSignal.signalHash("LIGHTS".getBytes(), 0) == signal.sid) {
                    if (signal.command == LinSignal.COMM_SET_VAR) {
                        // TODO
                    } else if (signal.command == LinSignal.COMM_WARN_VAR) {
                        // TODO
                    }

                }

            }
        };


        linBus = new LinBus() { // LinBus.java
            @Override
            public void receiveSignal(LinSignal signal) {
                Message msg = Message.obtain(usbInputHandler);
                msg.obj = signal;
                usbInputHandler.sendMessage(msg);
            }
        };

        usb_send_receive = new USB_Send_Receive();
        usb_send_receive.onCreate(this, usbInputHandler, linBus);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case 10:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
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

        usb_send_receive.onResume(getIntent());
    }


    @Override
    public void onPause() {
        super.onPause();

        usb_send_receive.onPause();
    }


    @Override
    public void onDestroy() {

        usb_send_receive.onDestroy(this);

        super.onDestroy();
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

