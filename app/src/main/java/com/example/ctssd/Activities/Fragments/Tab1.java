package com.example.ctssd.Activities.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.R;
import com.example.ctssd.Utils.Adapter;
import com.example.ctssd.Utils.DeviceDetailsObject;
import com.example.ctssd.Utils.UserObject;
import com.example.ctssd.Utils.Utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class Tab1 extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Adapter adapter;
    public static int serialNo = 1;
    private static ArrayList<UserObject> list = new ArrayList<>();
    private static HashMap<String, DeviceDetailsObject> map = new HashMap<>();

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

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

        IntentFilter intentFilter2 = new IntentFilter("REMOVE_OUT_OF_RANGE_DEVICES");
        Objects.requireNonNull(getActivity()).registerReceiver(updateListReceiver, intentFilter2);

        return root;
    }

    private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(action!=null && action.equals("ACTION_update_list"))
            {
                Log.i("Tab1 receiver", "updateListReceiver");
                String phone = intent.getStringExtra("phone");
                String distance = intent.getStringExtra("distance");
                int riskIndex = intent.getIntExtra("riskIndex", 0);
                boolean exists = false;

                for(UserObject object : list)
                {
                    if(object.getPhone().equals(phone)) {
                        list.remove(object);
                        break;
                    }
                }

                map.put(phone, new DeviceDetailsObject(Calendar.getInstance(), distance, riskIndex));
                list.add(new UserObject(phone, "Dist.-"+distance+"   RI-"+riskIndex));
                adapter.notifyDataSetChanged();
            }
            else if(action!=null && action.equals("REMOVE_OUT_OF_RANGE_DEVICES"))
            {
                for(HashMap.Entry element : map.entrySet())
                {
                    String ph = (String) element.getKey();
                    DeviceDetailsObject object = (DeviceDetailsObject) element.getValue();
                    if(Calendar.getInstance().getTime().getTime()-object.getTime().getTime().getTime()>10000)
                    {
                        map.remove(ph);
                        for(UserObject obj : list)
                        {
                            if(obj.getPhone().equals(ph)) {
                                list.remove(obj);
                                adapter.notifyDataSetChanged();
                                break;
                            }
                        }
                    }
                }
            }
        }
    };

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
            //getActivity().unregisterReceiver(receiver);
            Objects.requireNonNull(getActivity()).unregisterReceiver(updateListReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
