package team8.personaltransportation;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;


/**
 * Created by Joseph O on 2/1/2016.
 */
public class USB_Send_Receive {

    // variables for USB communication
    private static final String ACTION_USB_PERMISSION =    "team8.personaltransportation.action.USB_PERMISSION";
    public UsbManager UIusbManager;
    public UsbAccessory UIaccessory;
    public PendingIntent UIpermissionIntent;
    private boolean UIPermissionRequestPending = true;

    public ParcelFileDescriptor UIfileDescriptor;
    public FileInputStream UIinputStream;
    public FileOutputStream UIoutputStream;

    Thread testOutputThread;
    Thread testInputThread;
    Queue<USBMessage> inputQueue;
    Lock inputQueueLock = new ReentrantLock();
    Queue<USBMessage> outputQueue;
    Lock outputQueueLock = new ReentrantLock();

    USB_ACTIVITY_Thread UIhandlerThread;

    Handler UIHandler;
    Handler outputHandler;

    //static USBMessage usbMessage;

    public void onCreate(final FullscreenActivity activity) {

        // XXX Setup USB communication items  xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        UIusbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        Log.d("onCreate", "usbmanager: " + UIusbManager);
        UIpermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        Log.d("onCreate", "filter: " + filter);
        activity.registerReceiver(UIusbReceiver, filter);

        // Handles incoming messages
        UIHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                // XXX perform functionality to handle message (and provide response to USB with UIoutputStream.write())
                // XXX Temporary: spit back input data in message to USB

                Log.d("UIHandler", "handling message: " + msg);


                //final ImageButton warningButton = (ImageButton) activity.findViewById(R.id.warning);
                //warningButton.setImageResource(R.drawable.warningon); // Example that images can be set in these handlers


                FullscreenActivity.usbMessage = (USBMessage) msg.obj;

                // TEST _ JOSEPH
                if (FullscreenActivity.usbMessage.comm == USBMessage.COMM_SET_VAR) {
                    if (FullscreenActivity.usbMessage.sid == USBMessage.SID_BATTERY){
//                        int RecievedBatteryData_TEST = usbMessage.getData_asInt();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String Data_IN_TEST = FullscreenActivity.usbMessage.data.toString();
                                //String Data_String_TEST = Arrays.copyOfRange(FullscreenActivity.usbMessage.data, 0, 2).toString();
                                Toast.makeText(activity.getApplicationContext(), "TEST - CHANGED Battery::", Toast.LENGTH_SHORT).show();
                                if (FullscreenActivity.batButtonSwitch == 0) {
                                    FullscreenActivity.batButton.setImageResource(R.drawable.battery00);
                                    FullscreenActivity.batButtonSwitch = 1;
                                } else if (FullscreenActivity.batButtonSwitch == 1) {
                                    FullscreenActivity.batButton.setImageResource(R.drawable.battery10);
                                    FullscreenActivity.batButtonSwitch = 2;
                                } else if (FullscreenActivity.batButtonSwitch == 2) {
                                    FullscreenActivity.batButton.setImageResource(R.drawable.battery50);
                                    FullscreenActivity.batButtonSwitch = 3;
                                } else {
                                    FullscreenActivity.batButton.setImageResource(R.drawable.battery75);
                                    FullscreenActivity.batButtonSwitch = 0;
                                }
                            }
                        });
//                        FullscreenActivity.batButtonSwitch
//                        FullscreenActivity.batButton.setImageResource(R.drawable.battery00);
                    } else if (FullscreenActivity.usbMessage.sid == USBMessage.SID_LIGHTS) {

                    } else if (FullscreenActivity.usbMessage.sid == USBMessage.SID_SPEED) {

                    } else {

                    }

                }


                //byte[] bytes = new byte[4];
                //bytes[0] = 'b';
                //bytes[1] = 'c';
                //bytes[2] = 'd';
                //bytes[3] = 'e';

                //byte[] buffer = String.valueOf(usbMessage.type).getBytes();

                try {
                    UIoutputStream.write(FullscreenActivity.usbMessage.serialize(), 0, FullscreenActivity.usbMessage.length + USBMessage.MESSAGE_DATA_OFFSET);
                    //UIoutputStream.write(bytes);
                    UIoutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("UIHandler", "Error: could not write data to USB output");
                }

            }
        };

        // Handles outgoing messages (Currently not being used)
        outputHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                // XXX outputs a command through USB
                Log.d("outputHandler", "handling message: " + msg);

                byte[] temp;

                temp = (byte[]) msg.obj;
                try {
                    UIoutputStream.write(temp);
                    UIoutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("outputHandler", "Error: could not write data to USB output");
                }
            }
        };

    }


    // source : http://www.java2s.com/Open-Source/Android_Free_Code/Example/code/com_examples_accessory_controllerMainUsbActivity_java.htm
    public void onResume(Intent intent) {

        Log.d("onResume", "resuming...");
        if (UIinputStream != null && UIoutputStream != null) {
            return;
        }

        UsbAccessory[] accessories = UIusbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (UIusbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (UIusbManager) {
                    if (!UIPermissionRequestPending) {
                        UIusbManager.requestPermission(accessory,
                                UIpermissionIntent);
                        UIPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d("onResume", "accessory is null");
        }
    }

    public void onPause() {
        Log.d("onPause", "pausing...");
        closeAccessory();
    }

    public void onDestroy(Activity activity) {
        Log.d("onDestroy", "destroying...");

        activity.unregisterReceiver(UIusbReceiver);
    }

    // source : http://www.java2s.com/Open-Source/Android_Free_Code/Example/code/com_examples_accessory_controllerMainUsbActivity_java.htm
    private void openAccessory(UsbAccessory accessory) {

        Log.d("openAccessory", "trying to open UIaccessory: " + UIusbReceiver);

        UIfileDescriptor = UIusbManager.openAccessory(accessory);
        if (UIfileDescriptor != null) {
            UIaccessory = accessory;
            FileDescriptor fd = UIfileDescriptor.getFileDescriptor();
            UIinputStream = new FileInputStream(fd);
            UIoutputStream = new FileOutputStream(fd);

            /*
            testOutputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        outputQueueLock.lock();
                        USBMessage message = outputQueue.poll();
                        outputQueueLock.unlock();
                        try {
                            Thread.sleep(1000, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        USBMessage message = new USBMessage();
                        message.type = 10;
                        message.data = new byte[6];
                        message.data[0] = 'a';
                        message.data[1] = 'b';
                        message.data[2] = 'c';
                        message.data[3] = 'd';
                        message.data[4] = 'f';

                        if (message != null) {
                            int type = message.type;
                            byte[] typeBuffer = new byte[4];
                            typeBuffer[0] = (byte) (type >> 24);
                            typeBuffer[1] = (byte) (type >> 16);
                            typeBuffer[2] = (byte) (type >> 8);
                            typeBuffer[3] = (byte) (type);
                            byte[] lenBuffer = new byte[4];
                            lenBuffer[0] = (byte) (message.data.length >> 24);
                            lenBuffer[1] = (byte) (message.data.length >> 16);
                            lenBuffer[2] = (byte) (message.data.length >> 8);
                            lenBuffer[3] = (byte) (message.data.length);
                            try {
                                UIoutputStream.write(typeBuffer);
                                UIoutputStream.write(lenBuffer);
                                UIoutputStream.write(message.data);
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                }
            });
            //testOutputThread.start();

            testInputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        USBMessage message = new USBMessage();
                        byte[] buffer = new byte[4];

                        // Read message type
                        try {
                            UIinputStream.read(buffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message.type = (((int) buffer[0]) << 24)
                                & (((int) buffer[1]) << 16)
                                & (((int) buffer[2]) << 8)
                                & ((int) buffer[3]);

                        // Read message length
                        try {
                            UIinputStream.read(buffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int length = (((int) buffer[0]) << 24)
                                & (((int) buffer[1]) << 16)
                                & (((int) buffer[2]) << 8)
                                & ((int) buffer[3]);
                        // Read message data
                        message.data = new byte[length];
                        try {
                            UIinputStream.read(message.data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        outputQueueLock.lock();
                        outputQueue.add(message);
                        outputQueueLock.unlock();
                    }
                }
            });
            //testInputThread.start();
            */

            // create a thread, passing it the USB input stream and this task's handler object
            UIhandlerThread = new USB_ACTIVITY_Thread(UIHandler, UIinputStream);
            UIhandlerThread.start();
            Log.d("openAccessory", "opened UIaccessory: " + UIusbReceiver);
        } else {
            Log.d("openAccessory", "UIaccessory open fail: " + UIusbReceiver);
        }
    }

    // Close the connected UIaccessory (either unplugged or normal
    private void closeAccessory() {
        // try to close the UIinputStream, UIoutputStream, and UIfileDescriptor
        Log.d("closeAccessory", "closing accessory...");
        try {
            UIfileDescriptor.close();
        } catch (IOException ex) {}
        try {
            UIinputStream.close();
        } catch (IOException ex) {}
        try {
            UIoutputStream.close();
        } catch (IOException ex) {}

        // afterwards, set them all to null
        UIfileDescriptor = null;
        UIinputStream = null;
        UIoutputStream = null;

        // afterwards (?), stop execution of program
        System.exit(0); // XXX ??
    }

    /*
   * This receiver monitors for the event of a user granting permission to use
   * the attached UIaccessory.  If the user has checked to always allow, this will
   * be generated following attachment without further user interaction.
   * Source : http://www.java2s.com/Open-Source/Android_Free_Code/Example/code/com_examples_accessory_controllerMainUsbActivity_java.htm
   */
    private final BroadcastReceiver UIusbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("UIusbReceiver", "initializing...");
            String action = intent.getAction();

            Log.d("UIusbReceiver", "action: " + action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d("USB", "permission denied for UIaccessory "+ accessory);
                    }
                    UIPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(UIaccessory)) {
                    closeAccessory();
                }
            }
        }
    };

    // Private thread class inside of USB_Send_Receive class
    private class USB_ACTIVITY_Thread extends Thread {

        Handler USBhandler;
        FileInputStream USBInputStream;

        USB_ACTIVITY_Thread(Handler h, FileInputStream fI) {

            Log.d("USB_ACTIVITY_Thread", "initializing...");

            USBhandler = h;
            USBInputStream = fI;
        }

        // Receives input and handles it
        @Override
        public void run() {
            byte[] data_recieved = new byte[262];
            int data_recieved_len;

            while(true) {
                Log.d("USB_Activity_Thread_run", "running...");
                try {
                    if(USBInputStream != null) {
                        data_recieved_len = USBInputStream.read(data_recieved,0,262); // change later
                        if (data_recieved_len < USBMessage.MESSAGE_DATA_OFFSET) {
                            Log.d("USB_Activity_Thread_run", "Error: did not read enough data from USB");
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("USB_Activity_Thread_run", "Error: could not read data from USB");
                    break;
                }

                Message mss = Message.obtain(USBhandler); // XXX can also pass objects/input data with messages (obtain function is overloaded)
                //Message mss = Message.obtain(outputHandler);
                // XXX fill in this with input functionality (interpreting the input data stream, then calling a )
                // XXX
                // XXX
                // XXX Temporary: save the input data in the message (to spit back to USB device)
                USBMessage usbMessage = new USBMessage();
                usbMessage.create(data_recieved);
                //usbMessage.type = (int) data_recieved[0];

                mss.obj = usbMessage;
                //mss.obj =
                // afterwards, post message to handler (so main task can deal with data)
                //USBhandler.sendMessage(mss);

                USBhandler.handleMessage(mss);
                //outputHandler.handleMessage(mss);
            }
        }
    }

}


