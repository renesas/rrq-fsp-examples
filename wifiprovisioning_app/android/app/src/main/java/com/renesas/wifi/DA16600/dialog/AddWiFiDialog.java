package com.renesas.wifi.DA16600.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import com.renesas.wifi.DA16600.activity.DeviceControlActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;

public class AddWiFiDialog extends Dialog implements TextWatcher {

    private Context context;

    private EditText wifiNameEditText;
    private CheckBox security_open;
    private CheckBox security_wep;
    private CheckBox security_wpa;
    private CheckBox security_wpa2;
    private EditText passwordEditText;

    private Button okButton;
    private View passwordContainer;

    public AddWiFiDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.da16600_dialog_add_hidden_wifi);

        wifiNameEditText = (EditText) findViewById(R.id.wifiNameEditText);
        wifiNameEditText.addTextChangedListener(this);
        security_open = (CheckBox) findViewById(R.id.security_open);
        security_wep = (CheckBox) findViewById(R.id.security_wep);
        security_wpa = (CheckBox) findViewById(R.id.security_wpa);
        security_wpa2 = (CheckBox) findViewById(R.id.security_wpa2);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        okButton = (Button) findViewById(R.id.button_ok);
        okButton.setEnabled(false);
        passwordContainer = findViewById(R.id.password_container);
        passwordContainer.setVisibility(View.GONE);

        passwordEditText.addTextChangedListener(this);

        security_open.setChecked(false);
        security_open.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (security_open.isChecked()) {
                    security_open.setChecked(true);
                    security_wep.setChecked(false);
                    security_wpa.setChecked(false);
                    security_wpa2.setChecked(false);
                    StaticDataSave.networkSecurityNum = 0;
                } else {
                    security_open.setChecked(false);
                }

                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) DeviceControlActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                passwordContainer.setVisibility(View.GONE);

                passwordEditText.setText("");
                updateConnectButtonState();
                MyLog.i("onCheckedChanged: security_open [" + isChecked + "]");
            }
        });

        security_wep.setChecked(false);
        security_wep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (security_wep.isChecked()) {
                    security_open.setChecked(false);
                    security_wep.setChecked(true);
                    security_wpa.setChecked(false);
                    security_wpa2.setChecked(false);
                    StaticDataSave.networkSecurityNum = 1;
                } else {
                    security_wep.setChecked(false);
                }

                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) DeviceControlActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                passwordContainer.setVisibility(View.VISIBLE);

                passwordEditText.setText("");
                updateConnectButtonState();
                MyLog.i("onCheckedChanged: security_open [" + isChecked + "]");
            }
        });

        security_wpa.setChecked(false);
        security_wpa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (security_wpa.isChecked()) {
                    security_open.setChecked(false);
                    security_wep.setChecked(false);
                    security_wpa.setChecked(true);
                    security_wpa2.setChecked(false);
                    StaticDataSave.networkSecurityNum = 2;
                } else {
                    security_wpa.setChecked(false);
                }

                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) DeviceControlActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                passwordContainer.setVisibility(View.VISIBLE);

                passwordEditText.setText("");
                updateConnectButtonState();
                MyLog.i("onCheckedChanged: security_open [" + isChecked + "]");
            }
        });

        security_wpa2.setChecked(false);
        security_wpa2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (security_wpa2.isChecked()) {
                    security_open.setChecked(false);
                    security_wep.setChecked(false);
                    security_wpa.setChecked(false);
                    security_wpa2.setChecked(true);
                    StaticDataSave.networkSecurityNum = 3;
                } else {
                    security_wpa2.setChecked(false);
                }

                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) DeviceControlActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                passwordContainer.setVisibility(View.VISIBLE);

                passwordEditText.setText("");
                updateConnectButtonState();
                MyLog.i("onCheckedChanged: security_open [" + isChecked + "]");
            }
        });

        CheckBox cb = (CheckBox) findViewById(R.id.wifi_show_password);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    StaticDataSave.networkSecurity = true;
                } else {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    StaticDataSave.networkSecurity = false;
                }
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

                StaticDataSave.networkSSID = wifiNameEditText.getText().toString();
                StaticDataSave.networkPassword = passwordEditText.getText().toString();
                StaticDataSave.isHidden = 1;

                DeviceControlActivity.getInstance().displayNetworkinfo(
                        StaticDataSave.pingAddress,
                        StaticDataSave.svrAddress,
                        StaticDataSave.svrPort,
                        StaticDataSave.svrUrl
                );

                DeviceControlActivity.getInstance().displayAPinfo(
                        StaticDataSave.networkSSID,
                        StaticDataSave.networkSecurityNum,
                        StaticDataSave.networkPassword,
                        StaticDataSave.isHidden
                );

                DeviceControlActivity.getInstance().rl_scanAP.setVisibility(View.INVISIBLE);
                DeviceControlActivity.getInstance().ll_selectAP.setVisibility(View.VISIBLE);

                if (DeviceControlActivity.getInstance().btn_connect != null) {
                    DeviceControlActivity.getInstance().btn_connect.setText("Connect to "+StaticDataSave.networkSSID);
                }
            }
        });

        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                StaticDataSave.networkSSID = null;
                StaticDataSave.networkSecurity = false;
                StaticDataSave.networkPassword = null;
                StaticDataSave.isHidden = -1;
                DeviceControlActivity.getInstance().rl_scanAP.setVisibility(View.VISIBLE);
                DeviceControlActivity.getInstance().ll_selectAP.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Text change listener methods
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        //If the user chooses security as Open then make sure that the WiFi name is entered
        updateConnectButtonState();
    }

    private void updateConnectButtonState() {
        if (security_open.isChecked()) {
            okButton.setEnabled(wifiNameEditText.getText().length() > 0);
        } else {
            //If security is other than none make sure they enter both wifiname and password
            okButton.setEnabled(wifiNameEditText.getText().length() > 0 && passwordEditText.getText().length() > 0);
        }
    }

}