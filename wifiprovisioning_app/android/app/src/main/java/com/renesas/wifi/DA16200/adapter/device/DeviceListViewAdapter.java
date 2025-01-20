package com.renesas.wifi.DA16200.adapter.device;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.renesas.wifi.R;

import java.util.List;


public class DeviceListViewAdapter extends ArrayAdapter<DeviceRowItem> {

    Context context;

    public DeviceListViewAdapter(Context context, int resourceId, List<DeviceRowItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView iv_signalBar;
        TextView tv_level;
        TextView tv_ssid;
        TextView tv_mac;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        DeviceRowItem deviceRowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.wifi_list_item, null);
            holder = new ViewHolder();
            holder.iv_signalBar = (ImageView) convertView.findViewById(R.id.iv_signalBar);
            holder.tv_level = (TextView) convertView.findViewById(R.id.tv_level);
            holder.tv_ssid = (TextView) convertView.findViewById(R.id.tv_ssid);
            holder.tv_mac = (TextView) convertView.findViewById(R.id.tv_mac);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.tv_ssid.setText(deviceRowItem.getSSID());
        holder.tv_mac.setText(deviceRowItem.getMAC());
        holder.iv_signalBar.setImageResource(deviceRowItem.getImageId());
        holder.tv_level.setText(String.valueOf(deviceRowItem.getLevel()));

        return convertView;
    }

}
