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


public class DoorNotifyFragment extends Fragment {

    public static final String ARG_ITEM_ID = "doorNotify";

    Activity activity;
    public ListView notifyListView;
    public List<NotifyFormat> notifyFormat;
    public NotifyAdapter notifyAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.awsiot_fragment_dooraccess, container, false);
        notifyListView = (ListView)view.findViewById(R.id.list_dooraccess);

        loadingNotifyInfo();        // loading access info from cloud server or DB ???

        notifyAdapter = new NotifyAdapter(activity, R.layout.awsiot_adpater_notify, notifyFormat);
        notifyListView.setAdapter(notifyAdapter);

        return view;
    }

    private void loadingNotifyInfo() {

        // for demo
        String[] notifyDate = {"2018/07/13", "2018/07/16", "2018/07/19"};
        String[] notifyTime = {"10:51", "13:19", "18:26"};
        String[] notifyMsg = {"Registered DoorLock", "Registered My Home", "Registered Guest"};

        notifyFormat = new ArrayList<NotifyFormat>();

        for(int index = 0; index<notifyMsg.length; index++){
            notifyFormat.add(new NotifyFormat(index, notifyDate[index], notifyTime[index],notifyMsg[index]));
        }
    }

    public void updateAccessInfo(){     // periodically getting or notified from server

    }
}
