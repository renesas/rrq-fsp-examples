package com.renesas.wifi.awsiot.notify;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.renesas.wifi.R;

import java.util.List;


public class AccessAdapter extends ArrayAdapter<AccessFormat> {

    private Context context;
    public List<AccessFormat> accRecord;


    public AccessAdapter(Context _context, int resource, List<AccessFormat> _accRecord) {
        super(_context, resource, _accRecord);

        this.context = _context;
        this.accRecord = _accRecord;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.awsiot_adpater_access, null);

//        ImageView imgUserIcon = (ImageView)convertView.findViewById(R.id.user_type);    // icon not define yet
        TextView txtMsg = (TextView)convertView.findViewById(R.id.txt_accessMsg);
        TextView txtTime = (TextView)convertView.findViewById(R.id.txt_accessTime);

        AccessFormat accessFormat = (AccessFormat)getItem(position);

        txtMsg.setText(accessFormat.getAccessMessage());
        txtTime.setText(accessFormat.getAccessTime());


        return convertView;
    }

    @Override
    public void add(AccessFormat ind) {
        super.add(ind);
        accRecord.add(ind);
        notifyDataSetChanged();
    }

    @Override
    public void remove(AccessFormat ind) {
        super.remove(ind);
        accRecord.remove(ind);
        notifyDataSetChanged();
    }

    @Override
    protected void finalize() throws Throwable {
        free();
        super.finalize();
    }

    private void free(){
        context = null;
        accRecord = null;
    }
}
