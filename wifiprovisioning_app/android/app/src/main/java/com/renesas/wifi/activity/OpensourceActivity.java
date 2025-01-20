/**
 ****************************************************************************************
 *
 * @file OpensourceActivity.java
 *
 * @brief Opensource License Activity class
 *
 * Copyright (c) 2023. Renesas Electronics. All rights reserved.
 *
 * This software ("Software") is owned by Renesas Electronics.
 *
 * By using this Software you agree that Renesas Electronics retains all
 * intellectual property and proprietary rights in and to this Software and any
 * use, reproduction, disclosure or distribution of the Software without express
 * written permission or a license agreement from Renesas Electronics is
 * strictly prohibited. This Software is solely for use on or in conjunction
 * with Renesas Electronics products.
 *
 * EXCEPT AS OTHERWISE PROVIDED IN A LICENSE AGREEMENT BETWEEN THE PARTIES, THE
 * SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. EXCEPT AS OTHERWISE
 * PROVIDED IN A LICENSE AGREEMENT BETWEEN THE PARTIES, IN NO EVENT SHALL
 * RENESAS ELECTRONICS BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THE SOFTWARE.
 *
 ****************************************************************************************
 */
package com.renesas.wifi.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.renesas.wifi.R;

public class OpensourceActivity extends BaseActivity implements View.OnClickListener
{

    private static final String TAG = OpensourceActivity.class.getCanonicalName();

    private ImageView iv_back;
    private TextView mOpensourceLicenseTv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_opensource);

        onInitResource();
    }

    private void onInitResource()
    {
        Log.d(TAG, "onInitResource()");

        iv_back = findViewById(R.id.iv_back);
        mOpensourceLicenseTv = findViewById(R.id.OpensourceLicenseTv);

        iv_back.setOnClickListener(this);

        mOpensourceLicenseTv.setText(R.string.opensource_license_detail);
    }

    @Override
    public void onClick(View v)
    {

        switch (v.getId())
        {
            case R.id.iv_back:
                onBackPressed();
                break;
                default:
                    break;
        }
    }
}
