package com.renesas.wifi.DA16600;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.TextView;
import com.renesas.wifi.DA16600.activity.DeviceControlActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.util.StaticDataSave;

public class InputPasswordDialog extends Dialog implements TextWatcher {

    private Context context;
    private InputMethodManager imm;

    //UI resources
    private TextView wifiName;
    private EditText passwordEditText;
    private Button okButton;
    private View passwordContainer;

    public InputPasswordDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        setContentView(R.layout.da16600_dialog_inputpassword);

        imm = (InputMethodManager) DeviceControlActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);

        wifiName = (TextView) findViewById(R.id.wifiName);
        wifiName.setText(StaticDataSave.networkSSID);

        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        okButton = (Button) findViewById(R.id.button_connect);
        okButton.setEnabled(false);
        passwordContainer = findViewById(R.id.password_container);

        passwordContainer.setVisibility(View.VISIBLE);

        passwordEditText.setText("");

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(passwordEditText, 0);

        updateOkButtonState();

        passwordEditText.addTextChangedListener(this);

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

                InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);

                dismiss();

                StaticDataSave.networkPassword = passwordEditText.getText().toString();

                StaticDataSave.isHidden = 0;

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
        updateOkButtonState();
    }

    private void updateOkButtonState() {
        if (StaticDataSave.networkSecurity == true) {
            okButton.setEnabled(passwordEditText.getText().length() > 7);
        } else {
            okButton.setEnabled(true);
        }

    }
}