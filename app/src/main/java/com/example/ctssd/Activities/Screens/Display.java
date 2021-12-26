package com.example.ctssd.Activities.Screens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ctssd.R;
import com.example.ctssd.model.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Display extends Fragment {


    private Adapter adapter;
    private static ArrayList<Contact> list = new ArrayList<>();

    private OnFragmentInteractionListener mListener;
    private TextView tempCounter;
    private int deviceCounter = 0;

    public Display() {
    }

    public static Display newInstance(String param1, String param2) {
        Display fragment = new Display();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.fragment_tab1, container, false);

        ProgressBar progressBar = root.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

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

                String phone = intent.getStringExtra("phone");
                String distance = intent.getStringExtra("distance");
                int riskIndex = intent.getIntExtra("riskIndex", 0);

                for(Contact object : list)
                {
                    if(object.getPhone().equals(phone))
                    {
                        list.remove(object);
                        break;
                    }
                }

                list.add(new Contact(phone, distance, riskIndex, ""));
                adapter.notifyDataSetChanged();
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
            Objects.requireNonNull(getActivity()).unregisterReceiver(updateListReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class Adapter extends RecyclerView.Adapter< Adapter.ContactsViewHolder >
    {
        private List<Contact> contacts;
        private Context context;

        public Adapter(Context context, List<Contact> contacts)
        {
            this.context = context;
            this.contacts = contacts;
        }

        public class ContactsViewHolder extends RecyclerView.ViewHolder
        {
            private TextView deviceName, time;
            private RelativeLayout layout;

            public  ContactsViewHolder(View view)
            {
                super(view);
                deviceName = view.findViewById(R.id.id_deviceName);
                time = view.findViewById(R.id.id_time);
                layout = view.findViewById(R.id.item_question_layout);
            }
        }

        @NonNull
        @Override
        public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            return new ContactsViewHolder(LayoutInflater.from(context).inflate(R.layout.item_device, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ContactsViewHolder contactsViewHolder, final int i)
        {
            contactsViewHolder.deviceName.setText(contacts.get(i).getPhone());
            contactsViewHolder.time.setText("Dist.-"+contacts.get(i).getTime()+"  "+contacts.get(i).getRisk());
            contactsViewHolder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                { }
            });
        }

        @Override
        public int getItemCount()
        {
            return contacts.size();
        }
    }
}
