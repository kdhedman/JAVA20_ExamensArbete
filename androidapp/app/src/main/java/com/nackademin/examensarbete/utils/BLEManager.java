package com.nackademin.examensarbete.utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.nackademin.examensarbete.CfgActivity;
import com.nackademin.examensarbete.MainActivity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BLEManager{

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    TextView peripheralTextView;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String TAG = "BLE";
    private static final String INFO_UUID = "4bb31501-1936-4fc5-9816-8d1d18d85f81";
    private static final String INFO_NOTIFY_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String OUTPUT_UUID = "4bb31504-1936-4fc5-9816-8d1d18d85f81";
    private static final String SERVICE_UUID = "4bb31500-1936-4fc5-9816-8d1d18d85f81";

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    EditText deviceIndexInput;
    BluetoothGatt bluetoothGatt;

    private final static String IDPOINT_CHAR = "1401";
    private final static String TARGET_CHAR = "1402";
    private final static String DOORCTRL_CHAR = "1403";
    private final static String IDPOINT_CHOICE_CHAR = "1501";
    private final static String TARGET_CHOIC_CHAR = "1502";
    private final static String DOORCTRL_CHOICE_CHAR = "1503";
    private final static String DESCRIPTOR_UUID = "2ABE";
    private final static String IDPOINT_DESCR = "IDPoint";
    private final static String TARGET_DESCR = "Target";
    private final static String DOORCTROL_DESCR = "DoorController";

    TextView tvLog;
    BluetoothDevice bleDevice = null;
    public boolean isConnected = false;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String,String> uuids = new HashMap<>();

    //Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 2000;

    //private AppCompatActivity activity;
    private CfgActivity activity;

    public BLEManager(CfgActivity activity){
        this.activity = activity;
        this.tvLog = activity.getTvLog();
        onCreate();
    }

    protected void onCreate() {
        btManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if(btAdapter != null && !btAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enable, if not, propmt user to enable
        if(activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Need location access")
                    .setMessage("Just press ok, or it doesn't work")
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    })
                    .show();
        }

        CheckPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,   Manifest.permission.BLUETOOTH_ADMIN});

    }

    private void CheckPermissions(String[] aPermission)
    {
        ArrayList<String> aRequest = new ArrayList<String>();
        for (int i=0; i<aPermission.length; i++)
            if (ContextCompat.checkSelfPermission(activity,aPermission[i])
                    != PackageManager.PERMISSION_GRANTED)
                aRequest.add(aPermission[i]);

        if (aRequest.size() > 0)
            ActivityCompat.requestPermissions(activity, aRequest.toArray(new String[0]), 1);
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice() != null){
                if(result.getDevice().getName() != null) {
                    if (result.getDevice().getName().equalsIgnoreCase("NFC-EntryfyPad")) {
                        bleDevice = result.getDevice();
                        tvLog.append("Device found, rssi: " + result.getRssi() + "\n");
                        activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_CONNECTING);
                    }
                }
            }
        }
    };

    //Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //this will get called anytime you perform a read or write characteristic operation
            writeTvLog("Bluetooth Data recieved");
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
            BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUID.fromString(INFO_UUID));
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.interpretInfo(gattCharacteristic.getStringValue(0));
                        }
                    });
                }
            }, 500);

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            System.out.println(newState);
            switch (newState){
                case 0:
                    isConnected = false;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_DISCONNECTED);
                        }
                    });
                    break;
                case 2:
                    isConnected = true;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_CONNECTED);
                        }
                    });
                    //discover services and characteristics for this device
                    bluetoothGatt.discoverServices();
                    break;
                default:
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peripheralTextView.append("encountered unknown State\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            writeTvLog("device Service have been discovered.");
            displayGattServices(bluetoothGatt.getServices());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // Result of a characteristic read operation
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.interpretInfo(characteristic.getStringValue(0));
                }
            });
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(descriptor.getValue() != null){
                String descriptorContent = new String(descriptor.getValue(), StandardCharsets.UTF_8);
            }
        }
    };

    public boolean writeCharacteristic(String data){
        boolean status;
        if(bluetoothGatt == null){
            Log.e(TAG, "Write failed, lost connection");
            return false;
        }

        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("4bb31500-1936-4fc5-9816-8d1d18d85f81"));
        if(service == null){
            Log.e(TAG, "Write failed, service not found");
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString((OUTPUT_UUID)));
        if(characteristic == null){
            Log.e(TAG, "Write failed, characteristic not found");
        }

        characteristic.setValue(data);
        status = bluetoothGatt.writeCharacteristic(characteristic);
        return status;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices){
        if(gattServices == null) return;
        // Loops through available GATT Services.
        for(BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            //Loop through available Characteristics.
            for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                final String charUuid = gattCharacteristic.getUuid().toString();

                if(charUuid.equals(INFO_UUID)){
                    setCharacteristicNotification(gattCharacteristic, true);
                    bluetoothGatt.requestMtu(256);
                }
            }
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        writeTvLog("Setting up notification");
        if (INFO_UUID.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(INFO_NOTIFY_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
            Log.i(TAG, "Descriptor UUID: " + descriptor.getUuid().toString());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(bluetoothGatt.writeDescriptor(descriptor)){
                        writeTvLog("Notification setup successfully");
                    } else {
                        writeTvLog("Notification setup failed");
                    }
                }
            }, 500);

        }
    }

    public void startScanning(){
        btScanning = true;
        tvLog.setText("");
        tvLog.append("Started Scanning\n");
        activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_SCANNING);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
                if(bleDevice != null){
                    connectToDevice();
                } else {
                    activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_NO_DEVICE_FOUND);
                }
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        tvLog.append("Stopped scanning\n");
        btScanning = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void connectToDevice(){
        tvLog.append("Trying to connect to device... ");
        bluetoothGatt = bleDevice.connectGatt(activity, false, btleGattCallback);
    }

    public void disconnectDeviceSelected(){
        tvLog.append("Disconnecting from device\n");
        bleDevice = null;
        activity.setBtnBLEConnectionStatus(CfgActivity.CON_STATUS_DISCONNECTED);
        if(isConnected){
            bluetoothGatt.disconnect();
        }
    }

    private void writeTvLog(String message){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLog.append(message + "\n");
            }
        });
    }
}