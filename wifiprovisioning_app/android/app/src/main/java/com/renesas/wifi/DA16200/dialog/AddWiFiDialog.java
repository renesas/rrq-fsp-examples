package com.renesas.wifi.DA16200.dialog;

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

import com.renesas.wifi.DA16200.activity.SelectNetworkActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;

public class AddWiFiDialog extends Dialog implements TextWatcher {

    private Context context;

    private EditText wifiNameEditText;
    private CheckBox wifiRequiresAuth;
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

        setContentView(R.layout.da16200_dialog_add_hidden_wifi);

        wifiNameEditText = (EditText) findViewById(R.id.wifiNameEditText);
        wifiNameEditText.addTextChangedListener(this);
        wifiRequiresAuth = (CheckBox) findViewById(R.id.wifi_requires_auth);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        okButton = (Button) findViewById(R.id.button_ok);
        okButton.setEnabled(false);
        passwordContainer = findViewById(R.id.password_container);
        passwordContainer.setVisibility(View.GONE);

        passwordEditText.addTextChangedListener(this);

        wifiRequiresAuth.setChecked(false);
        wifiRequiresAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Hide any visible keyboards
                InputMethodManager imm = (InputMethodManager) SelectNetworkActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                passwordContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                passwordEditText.setText("");
                updateConnectButtonState();

                if (isChecked) {
                    StaticDataSave.networkSecurityNum = 3;
                } else {
                    StaticDataSave.networkSecurityNum = 0;
                }

                MyLog.i("onCheckedChanged: Wifi requires auth [" + isChecked + "]");
                MyLog.i("StaticDataSave.networkSecurityNum = "+StaticDataSave.networkSecurityNum);
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

                SelectNetworkActivity.getInstance().sendDPMSet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SelectNetworkActivity.getInstance().sendSSIDPW(
                        StaticDataSave.networkSSID,
                        StaticDataSave.networkPassword,
                        StaticDataSave.networkSecurityNum,
                        StaticDataSave.isHidden,
                        StaticDataSave.serverURL);
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
                StaticDataSave.networkSecurityNum = -1;
                StaticDataSave.isHidden = -1;
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
        if (!wifiRequiresAuth.isChecked()) {
            okButton.setEnabled(wifiNameEditText.getText().length() > 0);
        } else {
            //If security is other than none make sure they enter both wifiname and password
            okButton.setEnabled(wifiNameEditText.getText().length() > 0 && passwordEditText.getText().length() > 0);
        }
    }

}