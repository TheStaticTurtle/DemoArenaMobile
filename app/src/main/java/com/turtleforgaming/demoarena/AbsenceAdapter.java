package com.turtleforgaming.demoarena;

import android.app.Activity;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Created by Samuel on 26/07/2018.
 */


public class AbsenceAdapter extends ArrayAdapter<DemoArenaUtils.Absence> {

    public AbsenceAdapter(Context context, List<DemoArenaUtils.Absence> absences) {
        super(context, 0, absences);
    }

    private class AbsenceViewHolder{
        private TextView fromTV;
        private TextView toTV;
        private TextView causeTV;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.absences_adapter,parent, false);
        }

        AbsenceViewHolder viewHolder = (AbsenceViewHolder) convertView.getTag();
        if(viewHolder == null){
            viewHolder = new AbsenceViewHolder();
            viewHolder.fromTV = (TextView) convertView.findViewById(R.id.fromTV);
            viewHolder.toTV = (TextView) convertView.findViewById(R.id.toTV);
            viewHolder.causeTV = (TextView) convertView.findViewById(R.id.cause);
            convertView.setTag(viewHolder);
        }

        DemoArenaUtils.Absence abs = getItem(position);

        viewHolder.fromTV.setText(abs.from);
        viewHolder.toTV.setText(abs.to);
        viewHolder.causeTV.setText(abs.cause);

        if(abs.cause.equals("")) {
            viewHolder.causeTV.setText(getContext().getText(R.string.viewer_absences_cause_unknown));
        } else {
            viewHolder.causeTV.setText(abs.cause);
        }

        if (abs.justified) {
            viewHolder.fromTV.setTextColor(ContextCompat.getColor(getContext(), R.color.justifiedAbsenceTextColor));
            viewHolder.toTV.setTextColor(ContextCompat.getColor(getContext(), R.color.justifiedAbsenceTextColor));
            viewHolder.causeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.justifiedAbsenceTextColor));
        } else {
            viewHolder.fromTV.setTextColor(ContextCompat.getColor(getContext(), R.color.unjustifiedAbscenceTextColor));
            viewHolder.toTV.setTextColor(ContextCompat.getColor(getContext(), R.color.unjustifiedAbscenceTextColor));
            viewHolder.causeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.unjustifiedAbscenceTextColor));
        }
        return convertView;
    }

}
