package com.harshana.wposandroiposapp.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.harshana.wposandroiposapp.R;

import com.harshana.wposandroiposapp.UI.Fregments.IssuerItem;

import java.util.ArrayList;

/**
 * Created by indeevari_r on 3/17/2021.
 */
public  class TemporyAdapter extends ArrayAdapter<IssuerItem>{

    private final Context context;
    ArrayList<IssuerItem> rowBean;

    public TemporyAdapter(Context context, int resource, ArrayList<IssuerItem> objects) {
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

        IssuerItem item = rowBean.get(position);

        if(item != null) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.expandable_listview, null);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder = new ViewHolder();

            holder.textName = convertView.findViewById(R.id.textName);
            holder.textName.setText(item.issuerName);
            convertView.setTag(holder);

            if (item.isSelected()) {

                //holder.textName.setBackgroundColor(Color.WHITE);
                holder.textName.setBackgroundResource(R.drawable.edittext_whiterounded);
                holder.textName.setTextColor(Color.parseColor("#e33938"));
            } else {
                //holder.textName.setBackgroundColor(Color.parseColor("#e33938"));
                holder.textName.setBackgroundResource(R.drawable.button_shape_boc);
                holder.textName.setTextColor(Color.BLACK);
            }

        }

        return convertView;
    }

}
