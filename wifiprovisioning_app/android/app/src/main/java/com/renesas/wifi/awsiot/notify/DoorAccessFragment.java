package com.renesas.wifi.awsiot.notify;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.renesas.wifi.R;

import java.util.ArrayList;
import java.util.List;

public class DoorAccessFragment extends Fragment {
    public static final String ARG_ITEM_ID = "doorAccess";

    Activity activity;
    public ListView accessListView;
    public List<AccessFormat> accFormat;
    public AccessAdapter accessAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.awsiot_fragment_dooraccess, container, false);
        accessListView = (ListView)view.findViewById(R.id.list_dooraccess);

        loadingAccessInfo();        // loading access info from cloud server or DB ???

        accessAdapter = new AccessAdapter(activity, R.layout.awsiot_adpater_access,accFormat);
        accessListView.setAdapter(accessAdapter);


        return view;
    }

    private void loadingAccessInfo() {
        // for demo
        String[] accName = {"Master", "Guest", "User1"};
        int[] accUType = {0, 2, 1};
        int[] accAType = {1, 2, 0};
        String[] accDate = {"2018/07/12", "2018/07/15", "2018/07/18"};
        String[] accTime = {"11:25", "14:08", "16:37"};
        String[] accMsg = {"Master open the door", "go out", "Guest open the door"};

        accFormat = new ArrayList<AccessFormat>();

        for(int index = 0; index<accMsg.length; index++){
            accFormat.add(new AccessFormat(index, accName[index], accUType[index], accAType[index], accDate[index], accTime[index],accMsg[index]));
        }

    }

    public void updateAccessInfo(){     // periodically getting or notified from server

    }

}
