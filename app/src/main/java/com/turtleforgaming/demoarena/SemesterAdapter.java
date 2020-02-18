package com.turtleforgaming.demoarena;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Created by Samuel on 26/07/2018.
 */


public class SemesterAdapter extends ArrayAdapter<DemoArenaUtils.Semester> {

    public SemesterAdapter(Context context, List<DemoArenaUtils.Semester> semesters) {
        super(context, 0, semesters);
    }

    private class AbsenceViewHolder{
        private TextView id;
        private TextView name;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }


    private View createItemView(int position, View convertView, ViewGroup parent){
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.semester_adapter,parent, false);
        }

        AbsenceViewHolder viewHolder = (AbsenceViewHolder) convertView.getTag();
        if(viewHolder == null){
            viewHolder = new AbsenceViewHolder();
            viewHolder.id = (TextView) convertView.findViewById(R.id.semestreId);
            viewHolder.name = (TextView) convertView.findViewById(R.id.semesterName);
            convertView.setTag(viewHolder);
        }

        DemoArenaUtils.Semester sem = getItem(position);

        viewHolder.id.setText(sem.id);
        viewHolder.name.setText(sem.name);

        return convertView;
    }

}
