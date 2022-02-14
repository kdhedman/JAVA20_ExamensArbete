package com.nackademin.examensarbete;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.nackademin.examensarbete.utils.NFCManager;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private final String CARDNR_PREFERENCE = "cardNr";

    private ConstraintLayout authreqLayout;
    private ConstraintLayout mainLayout;
    private ImageButton btnAuthReq;
    private Button btnNavNfc;
    private Button btnNavCfg;
    private Button btnNfcSend;
    private EditText tfCardNr;
    private String cardNr;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private NFCManager nfcManager;
    private NfcAdapter nfcAdapter;

    private Variables vars = Variables.getInstance();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);

        authreqLayout = findViewById(R.id.authReqLayout);
        mainLayout = findViewById(R.id.mainLayout);

        btnAuthReq = findViewById(R.id.btnAuthReq);
        btnNavNfc = findViewById(R.id.btnNFC);
        btnNavCfg = findViewById(R.id.btnCfg);
        btnNfcSend = findViewById(R.id.btnNFCSend);
        tfCardNr = findViewById(R.id.tfCardNr);
        setupCardNrField();

        btnNavNfc.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.active_button));
        btnNavCfg.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.inactive_button));

        nfcManager = new NFCManager(this);

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
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

        btnNavCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), CfgActivity.class);
                startActivity(i);
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Scan fingerprint to access app")
                .setNegativeButtonText("Cancel")
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vars.setResumed(System.currentTimeMillis());
        if(!((vars.getResumed() - vars.getPaused()) < 1000)){
            vars.setAuthenticated(false);
        }

        if(vars.isAuthenticated()){
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

        disableButton("NFC not verified", true);

        try {
            if(nfcManager.verifyNFC()){
                enableButton();
                Intent nfcIntent = new Intent(this, getClass());
                nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);
                IntentFilter[] intentFiltersArray = new IntentFilter[]{};
                String[][] techList = //REDACTED;
                nfcAdapter = NfcAdapter.getDefaultAdapter(this);
                MainActivity thiis = this;
                nfcAdapter.enableForegroundDispatch(thiis, pendingIntent, intentFiltersArray, techList);
            } else {
                disableButton("NFC not verified", true);
            }
        } catch (NFCManager.NFCNonExisting error) {
            disableButton(error.getMessage(), false);
        } catch (NFCManager.NFCNotEnabled error) {
            disableButton(error.getMessage(), true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter!= null){
            nfcAdapter.disableForegroundDispatch(this);
        }
        vars.setPaused(System.currentTimeMillis());
    }

    private void setAuthreqLayoutOnTop(boolean isOnTop){
        if(isOnTop){
            authreqLayout.setVisibility(View.VISIBLE);
            mainLayout.setVisibility(View.INVISIBLE);
        } else {
            authreqLayout.setVisibility(View.INVISIBLE);
            mainLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setupCardNrField(){
        tfCardNr = findViewById(R.id.tfCardNr);

        if(sharedPreferences.contains(CARDNR_PREFERENCE))
            tfCardNr.setText(sharedPreferences.getString(CARDNR_PREFERENCE, ""));

        tfCardNr.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                sharedPreferences.edit().putString(CARDNR_PREFERENCE, editable.toString()).commit();
            }
        });
    }

    private void disableButton(String message, boolean setButtonEnabled){
        btnNfcSend.setOnClickListener(null);
        if(setButtonEnabled) {
            btnNfcSend.setEnabled(setButtonEnabled);
            btnNfcSend.setText(message + " Click for Settings");
            btnNfcSend.setAlpha(0.75f);
            btnNfcSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    }
                }
            });
        } else {
            btnNfcSend.setEnabled(setButtonEnabled);
            btnNfcSend.setText(message);
            btnNfcSend.setAlpha(0.5f);

        }
    }

    private void enableButton(){
        btnNfcSend.setAlpha(1f);
        btnNfcSend.setEnabled(true);
        btnNfcSend.setOnClickListener(null);
        btnNfcSend.setText(getString(R.string.button_nfc_send));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //REDACTED
        cardNr = tfCardNr.getText().toString();
        //REDACTED
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}