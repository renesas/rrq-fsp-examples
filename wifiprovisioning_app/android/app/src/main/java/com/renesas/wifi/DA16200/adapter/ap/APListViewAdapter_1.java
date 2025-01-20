package com.renesas.wifi.DA16200.adapter.ap;

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



public class APListViewAdapter_1 extends ArrayAdapter<APRowItem_1> {


    Context context;

    public APListViewAdapter_1(Context context, int resourceId, List<APRowItem_1> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView iv_signalBar;
        TextView tv_level;
        TextView txtTitle;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        APRowItem_1 apRowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.da16200_wifi_list_item, null);
            holder = new ViewHolder();
            holder.iv_signalBar = (ImageView) convertView.findViewById(R.id.iv_signalBar);
            holder.tv_level = (TextView) convertView.findViewById(R.id.tv_level);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.tv_name);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtTitle.setText(apRowItem.getSSID());
        holder.iv_signalBar.setImageResource(apRowItem.getImageId());
        holder.tv_level.setText(String.valueOf(apRowItem.getLevel()));

        return convertView;
    }
}
