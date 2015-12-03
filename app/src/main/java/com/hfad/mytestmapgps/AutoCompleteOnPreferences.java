package com.hfad.mytestmapgps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedList;


/**
 * Created by huynhducthanhphong on 12/2/15.
 */
public class AutoCompleteOnPreferences extends AutoCompleteTextView{
	///////////////
	// variable  //
	///////////////
    protected String mAppKey, mKey;

	/////////////////
	// Contructor  //
	/////////////////
    public AutoCompleteOnPreferences(Context context) {
        super(context);
    }

    public AutoCompleteOnPreferences(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public AutoCompleteOnPreferences(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    ///////////////////
    // public method //
    ///////////////////
    /**
     * set các biến cho obj là tên app gì và tên database gì cần lưu các chữ cái trong khung search
     * @param appKey   [tên app]
     * @param prefName [tên database cần lưu]
     */
    public void setPrefKeys(String appKey, String prefName) {
        mAppKey = appKey;
        mKey = prefName;
    }

    /**
     * lưu các chữ đã search vào database
     * @param context  Activity gì
     * @param value    lưu các chữ gì
     * @param appKey   lưu vào app nào
     * @param prefName lưu vào database nào
     */
    public static void storePreference(Context context, String value, String appKey, String prefName) {
        SharedPreferences prefs = context.getSharedPreferences(appKey, Context.MODE_PRIVATE);
        String prefValues = prefs.getString(prefName, "[]");
        JSONArray prefValuesArray;
        try {
            prefValuesArray = new JSONArray(prefValues);
            LinkedList<String> prefValuesList = new LinkedList<String>();

            for (int i=0; i<prefValuesArray.length(); i++){
                String prefValue = prefValuesArray.getString(i);
                if (!prefValue.equals(value))
                    prefValuesList.addLast(prefValue);
                //else, don't add it => it will be added at the beginning, as a new one...
            }
            //add the new one at the beginning:
            prefValuesList.addFirst(value);
            //remove last entry if too much:
            if (prefValuesList.size()>20)
                prefValuesList.removeLast();

            //Rebuild JSON string:
            prefValuesArray = new JSONArray();
            for (String s:prefValuesList){
                prefValuesArray.put(s);
            }
            prefValues = prefValuesArray.toString();
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(prefName, prefValues);
            ed.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    ////////////////////
    // private Method //
    ////////////////////
    @Override
    public boolean enoughToFilter() {
        return true;
    }


    @Override
    protected  void onFocusChanged(boolean focused, int direction, Rect previouslyFocusRect) {
        super.onFocusChanged(focused, direction, previouslyFocusRect);
        if (focused) {
            setPreferences();
            if (getAdapter() != null) {
                performFiltering(getText(), 0);
            }
        }
    }

    protected void setPreferences() {
        String[] prefs = getPreferences();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, prefs);
        this.setAdapter(adapter);
    }

    protected String[] getPreferences() {
        SharedPreferences prefs = getContext().getSharedPreferences(mAppKey, Context.MODE_PRIVATE);
        String prefString = prefs.getString(mKey, "[]");
        try {
            JSONArray prefArray = new JSONArray(prefString);
            String[] result = new String[prefArray.length()];
            for (int i=0; i<prefArray.length(); i++){
                result[i] = prefArray.getString(i);
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    
    





}
