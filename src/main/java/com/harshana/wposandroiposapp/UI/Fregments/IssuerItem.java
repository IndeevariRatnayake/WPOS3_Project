package com.harshana.wposandroiposapp.UI.Fregments;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MerchantSelect.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MerchantSelect#newInstance} factory method to
 * create an instance of this fragment.
 */


public class IssuerItem
{
    public String issuerName;
    public int issuerNumber;
    private boolean isSelected;

    public IssuerItem(int id,String n)
    {
        issuerNumber = id;
        issuerName = n;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}