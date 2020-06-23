package com.example.ctssd.Activities.Fragments;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Adapter;
import com.example.ctssd.Utils.DeviceDetailsObject;
import com.example.ctssd.Utils.UserObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Tab1 extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Adapter adapter;
    private static ArrayList<UserObject> list = new ArrayList<>();
    private static HashMap<String, DeviceDetailsObject> map = new HashMap<>();

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private TextView tempCounter;
    private int deviceCounter = 0;

    public Tab1() {
    }

    public static Tab1 newInstance(String param1, String param2) {
        Tab1 fragment = new Tab1();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.fragment_tab1, container, false);

        ProgressBar progressBar = root.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

//        root.findViewById(R.id.id_temp).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SharedPreferences settings = Objects.requireNonNull(getActivity()).getSharedPreferences("MySharedPref", getActivity().MODE_PRIVATE);
//                SharedPreferences.Editor editor = settings.edit();
//                editor.putInt("day", -1);
//                editor.apply();
//
//                SharedPreferences.Editor edit = settings.edit();
//                edit.putFloat("totalBTOnTime", 8);
//                edit.apply();
//            }
//        });

        tempCounter = root.findViewById(R.id.id_temp_count);

        RecyclerView recyclerView = root.findViewById(R.id.id_home_recyclerView);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager questionListLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(questionListLayoutManager);
        adapter = new Adapter(getActivity(), list);
        recyclerView.setAdapter(adapter);

        // Intent filter for updating device list. Broadcast will be sent from background process.
        IntentFilter intentFilter = new IntentFilter("ACTION_update_list");
        Objects.requireNonNull(getActivity()).registerReceiver(updateListReceiver, intentFilter);

//        IntentFilter intentFilter2 = new IntentFilter("REMOVE_OUT_OF_RANGE_DEVICES");
//        Objects.requireNonNull(getActivity()).registerReceiver(removeOutOfRangeDevices, intentFilter2);

        return root;
    }

    private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            String action = intent.getAction();
            if(action!=null && action.equals("ACTION_update_list"))
            {
                deviceCounter++;
                tempCounter.setText(String.valueOf(deviceCounter));

                Log.i("Tab1 receiver", "updateListReceiver");
                String phone = intent.getStringExtra("phone");
                String distance = intent.getStringExtra("distance");
                int riskIndex = intent.getIntExtra("riskIndex", 0);

                for(UserObject object : list)
                {
                    if(object.getPhone().equals(phone))
                    {
                        list.remove(object);
                        break;
                    }
                }

//                try {
//                    Log.i("Tab1", "Inserting in map ");
//                    map.put(phone, new DeviceDetailsObject(Calendar.getInstance(), distance, riskIndex));
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                }
//                finally {
//                    Log.i("Tab1", "Inserted in map ");
//                }

                list.add(new UserObject(phone, "Dist.-"+distance+"   RI-"+riskIndex));
                adapter.notifyDataSetChanged();
            }
        }
    };

//    private BroadcastReceiver removeOutOfRangeDevices = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent)
//        {
//            String action = intent.getAction();
//            if(action!=null && action.equals("REMOVE_OUT_OF_RANGE_DEVICES"))
//            {
//                try{
//                    HashMap<String, DeviceDetailsObject> tempMap = map;
//                    Log.i("Tab1", "Removing from map ");
//                    for(HashMap.Entry element : tempMap.entrySet())
//                    {
//                        String ph = (String) element.getKey();
//                        DeviceDetailsObject object = (DeviceDetailsObject) element.getValue();
//                        if(Calendar.getInstance().getTime().getTime()-object.getTime().getTime().getTime()>10000)
//                        {
//                            for(UserObject obj : list)
//                            {
//                                if(obj.getPhone().equals(ph)) {
//                                    list.remove(obj);
//                                    adapter.notifyDataSetChanged();
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                }
//                finally {
//                    Log.i("Tab1", "Removed from map");
//                }
//            }
//        }
//    };


    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            //Objects.requireNonNull(getActivity()).unregisterReceiver(removeOutOfRangeDevices);
            Objects.requireNonNull(getActivity()).unregisterReceiver(updateListReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
