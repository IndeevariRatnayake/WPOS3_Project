package com.harshana.wposandroiposapp.UI.Fregments;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.R;

import java.util.ArrayList;


public class IssuerMerchantSelectFragment extends Fragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public IssuerMerchantSelectFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DummySampleActivity.
     */
    // TODO: Rename and change types and number of parameters
    public static IssuerMerchantSelectFragment newInstance(String param1, String param2)
    {
        IssuerMerchantSelectFragment fragment = new IssuerMerchantSelectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        configDb = DBHelper.getInstance(getActivity().getApplicationContext());

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {

        super.onActivityCreated(savedInstanceState);
    }

    DBHelper configDb;

    ArrayList<IssuerItem> issuerList;
    ArrayList<MerchantItem> merchantList;

    CustomAdapterIssuer issuerAdapter = null;
    CustomAdapterMerchant merchantAdapter  = null;

    ListView lvIssuers = null;
    ListView lvMerchants = null;

    public void loadIssuers()
    {
        String issuerQueary = "SELECT * FROM IIT";
        Cursor issuers =  configDb.readWithCustomQuary(issuerQueary);

        if (issuers != null && issuers.getCount() == 0)
            return;

        issuerList = new ArrayList<>();
        issuerAdapter =  new CustomAdapterIssuer(getActivity().getApplicationContext(),android.R.layout.simple_expandable_list_item_1,issuerList);
        lvIssuers.setAdapter(issuerAdapter);

        //fill the array
        while (issuers.moveToNext())
        {
            IssuerItem item = new IssuerItem(issuers.getInt(issuers.getColumnIndex("IssuerNumber")), issuers.getString(issuers.getColumnIndex("IssuerLable")));
            issuerList.add(item);

        }
        issuerAdapter.notifyDataSetChanged();
        issuers.close();
    }


    //this method loads the existing merchants related to the selected issuer
    public void loadMerchants(int issuerNumber)
    {
        String merchantQuary = "select  mit.MerchantName,mit.MerchantNumber from tmif,mit where tmif.IssuerNumber = " + issuerNumber + " and  tmif.MerchantNumber = mit.MerchantNumber";

        Cursor merchants = configDb.readWithCustomQuary(merchantQuary);
        if (merchants != null && merchants.getCount() == 0)
            return;

        merchantList = new ArrayList<>();
        merchantAdapter = new CustomAdapterMerchant(getActivity().getApplicationContext(),android.R.layout.simple_expandable_list_item_1,merchantList);
        lvMerchants.setAdapter(merchantAdapter);

        while (merchants.moveToNext())
        {
            MerchantItem item =  new MerchantItem(merchants.getInt(merchants.getColumnIndex("MerchantNumber")),merchants.getString(merchants.getColumnIndex("MerchantName")));
            merchantList.add(item);

        }

        merchantAdapter.notifyDataSetChanged();
        merchants.close();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_merchant_select, container, false);

        // Inflate the layout for this fragment

        lvIssuers = v.findViewById(R.id.lvIssuer);
        lvMerchants = v.findViewById(R.id.lvMerchant);

        loadIssuers();

        lvIssuers.setOnItemClickListener(itemClickListener);
        lvMerchants.setOnItemClickListener(itemClickListener);
        return v;

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri)
    {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    int issuerID = -1;
    AdapterView.OnItemClickListener itemClickListener =  new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            if (parent == lvIssuers)
            {
                IssuerItem item = issuerList.get(position);
                issuerID = item.issuerNumber;
                loadMerchants(issuerID);
            }
            else if (parent == lvMerchants)
            {
                //notify the user with the data about selection made
                MerchantItem item =  merchantList.get(position);
                int merchId = item.merchantNumber;
                callSelectionMadeCallBack(issuerID,merchId);
            }
        }
    };

    private OnSelectionMade listener = null;

    public interface OnSelectionMade
    {
        void onSelectionMadeNotify(int issuerSelected, int merchantSelected);
    }

    public void setOnSelectionMadeListener(OnSelectionMade l)
    {
        if (l != null)
            listener = l;
    }

    void callSelectionMadeCallBack(int issSelected,int merchSelected)
    {
        if (listener != null)
            listener.onSelectionMadeNotify(issSelected,merchSelected);
    }

}
