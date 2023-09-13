package com.harshana.wposandroiposapp.UI;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.harshana.wposandroiposapp.UI.Fregments.MerchantItem;

import java.util.ArrayList;

import com.harshana.wposandroiposapp.R;


/**
 * Created by indeevari_r on 3/17/2021.
 */
public  class TemporyAdapterMerchant extends ArrayAdapter<MerchantItem>{

    private final Context context;
    ArrayList<MerchantItem> rowBean;

    public TemporyAdapterMerchant(Context context, int resource, ArrayList<MerchantItem> objects) {
        super(context, resource, objects);
        this.context = context;
        rowBean = objects;
    }


    private class ViewHolder {
        TextView textName;
    }

    ViewHolder holder = null;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        MerchantItem item = rowBean.get(position);

        if(item != null) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.expandable_listview, null);
                holder = new ViewHolder();

                holder.textName = convertView.findViewById(R.id.textName);
                holder.textName.setText(item.merchantName);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }


        }

        return convertView;
    }

}
