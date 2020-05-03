package com.example.ctssd.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ctssd.R;

public class Adapter extends RecyclerView.Adapter< Adapter.QuestionListViewHolder >
{
    private ArrayList<UserObject> questionList;
    private Context context;

    public Adapter(Context context, ArrayList<UserObject> questionList)
    {
        this.questionList = questionList;
        this.context = context;
    }

    public class QuestionListViewHolder extends RecyclerView.ViewHolder
    {
        private TextView deviceName, time;
        private RelativeLayout layout;

        public  QuestionListViewHolder(View view)
        {
            super(view);
            deviceName = (TextView) view.findViewById(R.id.id_deviceName);
            time = (TextView) view.findViewById(R.id.id_time);
            layout = (RelativeLayout) view.findViewById(R.id.item_question_layout);
        }
    }

    @NonNull
    @Override
    public QuestionListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
    {
        return new QuestionListViewHolder(LayoutInflater.from(context).inflate(R.layout.item_device, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionListViewHolder questionListViewHolder, final int i)
    {
        questionListViewHolder.deviceName.setText(questionList.get(i).getPhone());
        questionListViewHolder.time.setText(questionList.get(i).getTime());
        questionListViewHolder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return questionList.size();
    }
}