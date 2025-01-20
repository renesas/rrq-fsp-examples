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


public class NotifyAdapter extends ArrayAdapter<NotifyFormat> {

    private Context context;
    public List<NotifyFormat> notifyRecord;

    public NotifyAdapter(Context _context, int resource, List<NotifyFormat> _notifyRecord) {
        super(_context, resource, _notifyRecord);
        this.context = _context;
        this.notifyRecord = _notifyRecord;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.awsiot_adpater_notify, null);

        TextView txtMsg = (TextView)convertView.findViewById(R.id.txt_notifyMsg);
        TextView txtTime = (TextView)convertView.findViewById(R.id.txt_notifyTime);

        NotifyFormat notifyFormat = (NotifyFormat)getItem(position);

        txtTime.setText(notifyFormat.getNotifyTime());
        txtMsg.setText(notifyFormat.getNotifyMessage());


        return convertView;
    }

    @Override
    public void add(NotifyFormat ind) {
        super.add(ind);
        notifyRecord.add(ind);
        notifyDataSetChanged();
    }

    @Override
    public void remove( NotifyFormat ind) {
        super.remove(ind);
        notifyRecord.remove(ind);
        notifyDataSetChanged();
    }

    @Override
    protected void finalize() throws Throwable {
        free();
        super.finalize();
    }

    private void free(){
        context = null;
        notifyRecord = null;
    }
}
