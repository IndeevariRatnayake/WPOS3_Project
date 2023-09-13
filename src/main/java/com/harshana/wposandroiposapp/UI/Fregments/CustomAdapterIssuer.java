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

import java.util.ArrayList;

public class CustomAdapterIssuer extends ArrayAdapter<IssuerItem>
{

    ArrayList<IssuerItem> items;

    public CustomAdapterIssuer(@NonNull Context context, int resourceid, ArrayList<IssuerItem> items)
    {
        super(context, resourceid,items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {


        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_item, null);
        }

        IssuerItem item = items.get(position);

        if (item != null)
        {
            TextView vv = v.findViewById(R.id.txtListItem);
            if (vv != null)
                vv.setText(item.issuerName);
        }

        return  v;
    }
}
