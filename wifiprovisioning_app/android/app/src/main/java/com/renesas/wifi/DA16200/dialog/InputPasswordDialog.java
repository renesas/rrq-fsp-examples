package com.renesas.wifi.DA16200.dialog;

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

import com.renesas.wifi.DA16200.activity.SelectNetworkActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;

public class InputPasswordDialog extends Dialog {

    private Context context;

    private TextView wifiName;
    private EditText passwordEditText;
    private Button goButton;
    private View passwordContainer;

    public InputPasswordDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        setContentView(R.layout.da16200_dialog_inputpassword);

        wifiName = (TextView) findViewById(R.id.wifiName);
        wifiName.setText(StaticDataSave.networkSSID);

        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        goButton = (Button) findViewById(R.id.button_go);

        goButton.setEnabled(false);
        passwordContainer = findViewById(R.id.password_container);

        passwordContainer.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) SelectNetworkActivity.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);
        passwordEditText.setText("");
        updateGoButtonState();

        passwordEditText.addTextChangedListener(mTextEditorWatcher);


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

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();

                StaticDataSave.networkPassword = passwordEditText.getText().toString();

                SelectNetworkActivity.getInstance().sendDPMSet();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                StaticDataSave.isHidden = 0;

                SelectNetworkActivity.getInstance().sendSSIDPW(StaticDataSave.networkSSID, StaticDataSave.networkPassword, StaticDataSave.networkSecurityNum, StaticDataSave.isHidden, StaticDataSave.serverURL);
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

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            MyLog.i("passwordEditText.getText().length() = "+passwordEditText.getText().length());
            updateGoButtonState();
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private void updateGoButtonState() {
        MyLog.i("updateGoButtonState()");
        MyLog.i("StaticDataSave.networkSecurity = "+StaticDataSave.networkSecurity);
        MyLog.i("StaticDataSave.networkSecurityNum = "+StaticDataSave.networkSecurityNum);
        if ((StaticDataSave.networkSecurity == true) || StaticDataSave.networkSecurityNum > 0) {
            goButton.setEnabled(passwordEditText.getText().length() > 7);
            MyLog.i("goButton.setEnabled = "+goButton.isEnabled());
        } else {
            goButton.setEnabled(true);
        }
    }
}