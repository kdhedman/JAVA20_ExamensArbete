package com.nackademin.examensarbete.utils;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
//REDACTED
import android.util.Log;

import java.util.Locale;

public class NFCManager {
    private Activity activity;
    private NfcAdapter nfcAdapter;

    //REDACTED

    public NFCManager(Activity activity){
        this.activity = activity;
    }

    public static class NFCNonExisting extends Exception {
        public NFCNonExisting(){
            super();
        }
        public NFCNonExisting(String message){
            super(message);
        }
    }

    public static class NFCNotEnabled extends Exception {
        public NFCNotEnabled(){
            super();
        }
        public NFCNotEnabled(String message){
            super(message);
        }
    }

    public boolean verifyNFC() throws NFCNonExisting, NFCNotEnabled {

        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        if(nfcAdapter == null)
            throw new NFCNonExisting("No NFC on this device");

        if(!nfcAdapter.isEnabled())
            throw new NFCNotEnabled("NFC not enabled");

        return true;
    }

    //REDACTED
}