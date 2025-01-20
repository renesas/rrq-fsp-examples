package com.renesas.wifi.DA16600.adapter.ap;

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


public class APListViewAdapter extends ArrayAdapter<APRowItem> {


    Context context;

    public APListViewAdapter(Context context, int resourceId, List<APRowItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView iv_signalBar;
        TextView tv_level;
        TextView txtTitle;
        TextView tv_security;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        APRowItem apRowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.da16600_ap_list_item, null);
            holder = new ViewHolder();
            holder.iv_signalBar = (ImageView) convertView.findViewById(R.id.iv_signalBar);
            holder.tv_level = (TextView) convertView.findViewById(R.id.tv_level);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.tv_name);
            holder.tv_security = (TextView) convertView.findViewById(R.id.tv_security);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtTitle.setText(apRowItem.getSSID());
        holder.tv_security.setText(apRowItem.getStringSecurity());
        holder.iv_signalBar.setImageResource(apRowItem.getImageId());
        holder.tv_level.setText(String.valueOf(apRowItem.getLevel()));

        return convertView;
    }
}
