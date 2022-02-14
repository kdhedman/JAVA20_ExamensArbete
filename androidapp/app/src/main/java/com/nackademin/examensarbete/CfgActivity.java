package com.nackademin.examensarbete;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.nackademin.examensarbete.utils.BLEManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

public class CfgActivity extends AppCompatActivity {
    private ConstraintLayout layoutAuthReq;

    private ConstraintLayout layoutCfg;

    private ImageButton btnAuthReq;

    //For Token-layout in view
    private ConstraintLayout layoutTokens;
    private Button btnBLEConnectionStatus;
    private Button btnNavCfg, btnNavNfc, btnAccessDoor, btnExitConfig;
    private ImageButton btnIdPointRefresh, btnIdPointSend, btnTargetRefresh, btnTargetSend;
    private TextView tvLog;
    private Spinner spnIdPoint, spnDoor;

    // for IPCONF layout in view
    private ConstraintLayout layoutAuth;
    private EditText tfUsername, tfPassword;
    private EditText[] tfIP, tfDoorCtrlIP, tfGateway, tfDNS, tfSubnet;
    private EditText tfDoorCtrlIPPort;
    private Button btnConfSendAll;
    private Switch swDHCP;
    private CheckBox cbUsernameSend, cbPasswordSend, cbIPSend, cbDoorCtrlIPSend, cbDHCPSend, cbGatewaySend, cbDNSSend, cbSubnetSent;
    private List<CheckBox> checkBoxes = new ArrayList<>();

    private Executor executor;

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    // för BLE
    private BLEManager bleManager;

    private Variables vars = Variables.getInstance();

    private void bindGUIComponents() {
        // LAYOUTS
        layoutAuthReq = findViewById(R.id.authReqLayout);
        layoutCfg = findViewById(R.id.cfgLayout);
        layoutTokens = findViewById(R.id.layoutTokens);
        layoutAuth = findViewById(R.id.layoutAuth);

        // BIOMETRIC AUTH
        btnAuthReq = findViewById(R.id.btnAuthReq);

        // TITLE BAR
        btnNavCfg = findViewById(R.id.btnCfg);
        btnNavCfg.setBackgroundColor(getColor(R.color.active_button));
        btnNavCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchLayout();
            }
        });
        btnNavNfc = findViewById(R.id.btnNFC);
        btnNavNfc.setBackgroundColor(getColor(R.color.inactive_button));
        btnNavNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
        });


        // BOTTOM BUTTONS
        btnBLEConnectionStatus = findViewById(R.id.btnBLEConnectionStatus);
        btnBLEConnectionStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleManager.disconnectDeviceSelected();
            }
        });


        // TOKENS GUI
        tvLog = findViewById(R.id.tvLog);
        tvLog.setMovementMethod(new ScrollingMovementMethod());


        btnIdPointRefresh = findViewById(R.id.btnIdPointRefresh);
        btnIdPointRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleManager.writeCharacteristic(vars.BLE_UPDATE_IDPOINT);
            }
        });
        btnIdPointSend = findViewById(R.id.btnIdPointSend);
        btnIdPointSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendIdPointToken();
                String idPoint = spnIdPoint.getSelectedItem().toString();

                tvLog.append("Sending IDPoint Token: " + idPoint + "\n");
                bleManager.writeCharacteristic(vars.BLE_IDPOINT_CHOICE + idPoint);
            }
        });
        btnTargetRefresh = findViewById(R.id.btnTargetRefresh);
        btnTargetRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleManager.writeCharacteristic(vars.BLE_UPDATE_DOORS);
            }
        });
        btnTargetSend = findViewById(R.id.btnTargetSend);
        btnTargetSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendTargetToken();
                String door = spnDoor.getSelectedItem().toString();
                tvLog.append("Sending Target Door Token: " + door + "\n");
                bleManager.writeCharacteristic(vars.BLE_DOOR_CHOICE + door);
            }
        });
        btnAccessDoor = findViewById(R.id.btnAccessDoor);
        btnAccessDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleManager.writeCharacteristic(vars.BLE_ACCESS_DOOR);
            }
        });

        String[] spnIdPointDefaultVars = {getResources().getString(R.string.spinner_idpoint_default)};
        //    private String[] spnTargetDoorTestVars = {"TARGET1-TOKEN-11111-11111", "TARGET2-TOKEN-22222-22222", "TARGET3-TOKEN-33333-33333"};
        String[] spnTargetDoorDefaultVars = {getResources().getString(R.string.spinner_doors_default)};

        spnIdPoint = findViewById(R.id.spnIdPoint);
        setupSpinner(spnIdPoint, spnIdPointDefaultVars);
        spnDoor = findViewById(R.id.spnTargetPoint);
        setupSpinner(spnDoor, spnTargetDoorDefaultVars);

        btnExitConfig = findViewById(R.id.btnExitConfigMode);
        btnExitConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleManager.writeCharacteristic(vars.BLE_EXIT_CONFIG_MODE);
            }
        });

        // AUTH GUI
        tfUsername = findViewById(R.id.tfUsername);
        tfPassword = findViewById(R.id.tfPassword);
        tfIP = bindIPFields("tfIP");
        tfDoorCtrlIP = bindIPFields("tfDoorCtrlIP");
        tfGateway = bindIPFields("tfGateway");
        tfDNS = bindIPFields("tfDNS");
        tfSubnet = bindIPFields("tfSubnet");
        tfDoorCtrlIPPort = findViewById(R.id.tfDoorCtrlIPPort);
        tfDoorCtrlIPPort.setFilters(new InputFilter[]{new InputFilterMinMax(0, 65535)});
        btnConfSendAll = findViewById(R.id.btnConfSendAll);
        btnConfSendAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCheckedItems();
            }
        });
        swDHCP = findViewById(R.id.swDHCP);
        cbUsernameSend = findViewById(R.id.cbUsernameSend);
        checkBoxes.add(cbUsernameSend);
        cbPasswordSend = findViewById(R.id.cbPasswordSend);
        checkBoxes.add(cbPasswordSend);
        cbIPSend = findViewById(R.id.cbIPSend);
        checkBoxes.add(cbIPSend);
        cbDoorCtrlIPSend = findViewById(R.id.cbDoorCtrlIPSend);
        checkBoxes.add(cbDoorCtrlIPSend);
        cbDHCPSend = findViewById(R.id.cbDHCPSend);
        checkBoxes.add(cbDHCPSend);
        cbGatewaySend = findViewById(R.id.cbGatewaySend);
        checkBoxes.add(cbGatewaySend);
        cbDNSSend = findViewById(R.id.cbDNSSend);
        checkBoxes.add(cbDNSSend);
        cbSubnetSent = findViewById(R.id.cbSubnetSend);
        checkBoxes.add(cbSubnetSent);

        setTestValues();

    }

    private void setTestValues(){
        tfUsername.setText("root");
        cbUsernameSend.setChecked(true);
        tfPassword.setText("pass");
        cbPasswordSend.setChecked(true);
        tfIP[0].setText("10");
        tfIP[1].setText("144");
        tfIP[2].setText("11");
        tfIP[3].setText("58");
        cbIPSend.setChecked(true);
        tfDoorCtrlIP[0].setText("10");
        tfDoorCtrlIP[1].setText("144");
        tfDoorCtrlIP[2].setText("11");
        tfDoorCtrlIP[3].setText("55");
        tfDoorCtrlIPPort.setText("80");
        cbDoorCtrlIPSend.setChecked(true);
    }

    private EditText[] bindIPFields(String id) {
        EditText[] result = new EditText[4];
        for (int i = 1; i < 5; i++) {
            int resID = getResources().getIdentifier(id + i, "id", getPackageName());
            result[i - 1] = findViewById(resID);
            result[i - 1].setFilters(new InputFilter[]{new InputFilterMinMax(0, 255)});
        }
        return result;
    }

    private void setListeners() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cfg);

        bindGUIComponents();
        setListeners();

        //bleManager = new BLEManager(this, tvLog);
        bleManager = new BLEManager(this);

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(CfgActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                vars.setAuthenticated(true);
                setAuthreqLayoutOnTop(false);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                vars.setAuthenticated(false);
            }
        });
        bleManager.startScanning();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vars.setResumed(System.currentTimeMillis());
        if (!((vars.getResumed() - vars.getPaused()) < 1000)) {
            vars.setAuthenticated(false);
        }

        if (vars.isAuthenticated()) {
            setAuthreqLayoutOnTop(false);
        } else {
            setAuthreqLayoutOnTop(true);
            btnAuthReq.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    biometricPrompt.authenticate(promptInfo);
                }
            });
            biometricPrompt.authenticate(promptInfo);
        }
    }

    @Override
    protected void onPause() {
        vars.setPaused(System.currentTimeMillis());
        super.onPause();
    }

    public void interpretInfo(String info) {
        Log.w("INTERPRET", "BASEINFO: " + info);
        if (info != null && !info.isEmpty() && info.length() > 2) {
            char topic = info.charAt(0);
            Log.e("INTERPRET", "CHAR: " + topic);
            info = info.substring(2);

            if (topic == vars.BLE_UPDATE_IDPOINT.charAt(0)) {
                Log.w("INTERPRET", "INFO: " + info);
                String idPoints[] = info.split(vars.DELIMITER_SUB);
                setupSpinner(spnIdPoint, idPoints);
            } else if (topic == vars.BLE_UPDATE_DOORS.charAt(0)){
                Log.w("INTERPRET", "INFO: " + info);
                String doors[] = info.split(vars.DELIMITER_SUB);
                setupSpinner(spnDoor, doors);
            }
        }
    }

    private void setAuthreqLayoutOnTop(boolean isOnTop) {
        if (isOnTop) {
            layoutAuthReq.setVisibility(View.VISIBLE);
            layoutCfg.setVisibility(View.INVISIBLE);
        } else {
            layoutAuthReq.setVisibility(View.INVISIBLE);
            layoutCfg.setVisibility(View.VISIBLE);
        }
    }

    private void switchLayout() {
        if (layoutTokens.getVisibility() < 1) {
            layoutTokens.setVisibility(View.INVISIBLE);
            layoutAuth.setVisibility(View.VISIBLE);
        } else {
            layoutTokens.setVisibility(View.VISIBLE);
            layoutAuth.setVisibility(View.INVISIBLE);
        }
    }

    private void sendCheckedItems() {
        String dataToSend = "";
        final String DELIMITER = vars.DELIMITER;
        StringBuilder sb = new StringBuilder();
        sb.append(vars.BLE_IP_CONFIG);

        for (CheckBox cb : checkBoxes) {
            if (cb.isChecked()) {
                if (cb.getId() == cbUsernameSend.getId()) {
                    sb.append("u:" + tfUsername.getText().toString() + DELIMITER);
                } else if (cb.getId() == cbPasswordSend.getId()) {
                    sb.append("w:" + tfPassword.getText().toString() + DELIMITER);
                } else if (cb.getId() == cbIPSend.getId()) {
                    sb.append("i:" + validateIP(tfIP) + DELIMITER);
                } else if (cb.getId() == cbDoorCtrlIPSend.getId()) {
                    String port;
                    if (tfDoorCtrlIPPort.getText() == null || tfDoorCtrlIPPort.getText().toString().isEmpty()) {
                        port = "0";
                    } else {
                        port = tfDoorCtrlIPPort.getText().toString();
                    }
                    sb.append("h:" + validateIP(tfDoorCtrlIP) + DELIMITER + "p:" + port + DELIMITER);
                } else if (cb.getId() == cbDHCPSend.getId()) {
                    sb.append("c:" + (swDHCP.isChecked() ? "1" : "0") + DELIMITER);
                } else if (cb.getId() == cbGatewaySend.getId()) {
                    sb.append("g:" + validateIP(tfGateway) + DELIMITER);
                } else if (cb.getId() == cbDNSSend.getId()) {
                    sb.append("d:" + validateIP(tfDNS) + DELIMITER);
                } else if (cb.getId() == cbSubnetSent.getId()) {
                    sb.append("s:" + validateIP(tfSubnet) + DELIMITER);
                }
            }
        }
        if (sb.length() <= 0) {
            sb.append(",");
        }
        dataToSend = sb.toString() + DELIMITER;
        Log.e("GATT", "Message prepare, trying to send " + dataToSend);
        bleManager.writeCharacteristic(dataToSend);
    }

    private String validateIP(TextView[] ipArray) {
        String result = "";
        for (TextView tv : ipArray) {
            if (tv.getText().toString() == null || tv.getText().toString().equals("")) {
                tv.setText("0");
            }
            result += tv.getText().toString() + ".";
        }
        result = result.substring(0, result.length() - 1);
        return result;
    }

    private void setupSpinner(Spinner sourceSpinner, String[] content) {
        final Spinner spinner = sourceSpinner;
//        ArrayAdapter<String> ar = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, content);
        ArrayAdapter<String> ar = new ArrayAdapter<>(this, R.layout.spinner_theme, content);
        spinner.setAdapter(ar);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getApplicationContext(), content[position], Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public final static int CON_STATUS_NO_DEVICE_FOUND = 0;
    public final static int CON_STATUS_SCANNING = 1;
    public final static int CON_STATUS_CONNECTING = 2;
    public final static int CON_STATUS_CONNECTED = 3;
    public final static int CON_STATUS_DISCONNECTED = 4;

    public void setBtnBLEConnectionStatus(int status) {
        btnBLEConnectionStatus.setOnClickListener(null);
        switch (status) {
            case CON_STATUS_NO_DEVICE_FOUND: {
                btnBLEConnectionStatus.setText(getResources().getString(R.string.BLE_button_constatus_devicenotfound));
                btnBLEConnectionStatus.setEnabled(true);
                btnBLEConnectionStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bleManager.startScanning();
                    }
                });
                break;
            }
            case CON_STATUS_SCANNING: {
                btnBLEConnectionStatus.setText(getResources().getString(R.string.BLE_button_constatus_scanning));
                btnBLEConnectionStatus.setEnabled(false);
                break;
            }
            case CON_STATUS_CONNECTING: {
                btnBLEConnectionStatus.setText(getResources().getString(R.string.BLE_button_constatus_connecting));
                btnBLEConnectionStatus.setEnabled(false);
                break;
            }
            case CON_STATUS_CONNECTED: {
                btnBLEConnectionStatus.setText(getResources().getString(R.string.BLE_button_constatus_working));
                btnBLEConnectionStatus.setEnabled(true);
                btnBLEConnectionStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bleManager.disconnectDeviceSelected();
                    }
                });
                break;
            }
            case CON_STATUS_DISCONNECTED: {
                btnBLEConnectionStatus.setText(getResources().getString(R.string.BLE_button_constatus_disconnected));
                btnBLEConnectionStatus.setEnabled(true);
                btnBLEConnectionStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bleManager.startScanning();
                    }
                });
                break;
            }
            default: {
                btnBLEConnectionStatus.setEnabled(false);
                btnBLEConnectionStatus.setText("ERROR: " + status + "?");
                break;
            }
        }
        ;
    }

    public TextView getTvLog() {
        return this.tvLog;
    }
}
