package com.example.ctssd.Activities.Fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ctssd.R;
import com.example.ctssd.Utils.Adapter;
import com.example.ctssd.Utils.DatabaseHelper;
import com.example.ctssd.Utils.UserObject;
import com.example.ctssd.Utils.Utilities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class Tab1 extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static int todaysContacts = 0;
    private static final int REQUEST_ENABLE_BT = 1;
    private ProgressBar progressBar;
    private BluetoothAdapter bluetoothAdapter;
    DatabaseHelper myDb;

    public Adapter adapter;
    public static int serialNo = 1;
    public static ArrayList<UserObject> list = new ArrayList<>();
    //public static ArrayList<String> names = new ArrayList<>();
    private long mTime;

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

        progressBar = root.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        myDb = new DatabaseHelper(getActivity());

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.id_home_recyclerView);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager questionListLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(questionListLayoutManager);
        adapter = new Adapter(getActivity(), list);
        recyclerView.setAdapter(adapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Intent filter for device discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, filter);

        bluetoothAdapter.startDiscovery();

        // Intent filter for updating device list. Broadcast will be sent from background process.
        IntentFilter intentFilter = new IntentFilter("ACTION_update_list");
        getActivity().registerReceiver(updateListReceiver, intentFilter);

        return root;
    }

    private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getAction();
            Log.i("Tab1 receiver", message);
            list.clear();
            adapter.notifyDataSetChanged();
            //names.clear();
            serialNo=1;
        }
    };

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String mac = device.getAddress();
                String phone = device.getName();
                if(mac==null || mac.equals(""))
                    mac = "null";

                Calendar calendar = Calendar.getInstance();
                String time = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);
                Log.i("Tab1", "device found : "+serialNo);
                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                int iRssi = abs(rssi);
                double power = (iRssi - 59) / 25.0;
                String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                boolean exists = false;
                if(Utilities.isPhoneNoValid(phone))
                {
                    for(UserObject object : list )
                    {
                        if(object.getPhone().equals(phone))
                        {
                            exists = true;
                        }
                    }
                    if( !exists )
                    {
                        list.add(new UserObject(phone, mac+"    Dist.- "+mm+"m"));
                        serialNo++;
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            getActivity().unregisterReceiver(receiver);
            getActivity().unregisterReceiver(updateListReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
