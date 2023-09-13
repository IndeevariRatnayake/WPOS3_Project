package com.harshana.wposandroiposapp.UI.Fregments;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;

import java.util.ArrayList;


public class CustomAdapterMerchant extends ArrayAdapter<MerchantItem>
{

    ArrayList<MerchantItem> items;

    public CustomAdapterMerchant(@NonNull Context context, int resourceid, ArrayList<MerchantItem> items)
    {
        super(context, resourceid, items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_item, null);
        }

        MerchantItem item = items.get(position);

        if (item != null) {
            TextView vv = v.findViewById(R.id.txtListItem);
            if (vv != null)
                vv.setText(item.merchantName);
        }

        return v;
    }
}



class CustomHostAdapter extends ArrayAdapter<String>
{
    ArrayList<String> items;

    public CustomHostAdapter(@androidx.annotation.NonNull Context context, int resource, @androidx.annotation.NonNull ArrayList<String> items)
    {
        super(context, resource, items);
        this.items = items;
    }


    @Override
    public View getView(int position, @androidx.annotation.Nullable View convertView, @androidx.annotation.NonNull ViewGroup parent)
    {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_layout_host, null);
        }

        String host = items.get(position);

        if (host != null) {
            TextView t = v.findViewById(R.id.txtListItemHost);
            if (t != null)
            {
                t.setText(items.get(position));
                if (SettingsInterpreter.isSingleMerchantEnabled())
                    t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                return t;
            }
        }

        return null;
    }
}
