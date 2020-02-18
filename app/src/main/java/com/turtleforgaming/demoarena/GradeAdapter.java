package com.turtleforgaming.demoarena;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.List;


public class GradeAdapter extends ArrayAdapter<DemoArenaUtils.Grade> {

    public GradeAdapter(Context context, List<DemoArenaUtils.Grade> grades) {
        super(context, 0, grades);
    }

    private class GradeViewHolder{
        private TextView TVgradeID;
        private TextView TVgradeName;
        private TextView TVgrade;
        private TextView TVgradeStats;
        private TextView TVgradeStatsTitle;
        private ConstraintLayout layout;
    }

    int topx(int dip) {
        Resources r = getContext().getResources();
        return (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.grade_adapter,parent, false);
        }

        GradeViewHolder viewHolder = (GradeViewHolder) convertView.getTag();
        if(viewHolder == null){
            viewHolder = new GradeViewHolder();
            viewHolder.TVgradeID = (TextView) convertView.findViewById(R.id.TVgradeID);
            viewHolder.TVgradeName = (TextView) convertView.findViewById(R.id.TVgradeName);
            viewHolder.TVgrade = (TextView) convertView.findViewById(R.id.TVgrade);
            viewHolder.TVgradeStats = (TextView) convertView.findViewById(R.id.TVgradeStats);
            viewHolder.layout = (ConstraintLayout) convertView.findViewById(R.id.gradeLayout);
            viewHolder.TVgradeStatsTitle = (TextView) convertView.findViewById(R.id.TVgradeStatsTitle);
            convertView.setTag(viewHolder);
        }

        DemoArenaUtils.Grade grade = getItem(position);


        if(grade.type.contains( "GRADE"))  {
            viewHolder.layout.setPadding(topx(24),topx(10), topx(8),topx(10));
            viewHolder.layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bgColorGrade));
        }
        if(grade.type.contains("COURSE")) {
            viewHolder.layout.setPadding(topx(16),topx(10), topx(8),topx(10));
            viewHolder.layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bgColorCourse));
        }
        if(grade.type.contains("UE"))     {
            viewHolder.layout.setPadding(topx(8),topx(10), topx(8),topx(10));
            viewHolder.layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bgColorUE));
        }
        if(grade.type.contains("MOY") || grade.type.contains("BONUS"))     {
            viewHolder.layout.setPadding(topx(16),topx(10), topx(8),topx(10));
            viewHolder.layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bgColorMOY));
        }
        if(grade.type.contains("MOYGEN"))     {
            viewHolder.layout.setPadding(topx(8),topx(10), topx(8),topx(10));
            viewHolder.layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bgColorMOYGENE));
        }

        int textColor = ContextCompat.getColor(getContext(), R.color.black);
        viewHolder.TVgradeID.setTextColor(textColor);
        viewHolder.TVgradeName.setTextColor(textColor);

        viewHolder.TVgradeID.setText(grade.id);
        viewHolder.TVgradeName.setText(grade.name);

        String gradeText = String.valueOf(grade.grade);
        if(grade.outof >0) { gradeText +="/" + grade.outof; }
        viewHolder.TVgrade.setText(gradeText);

        String statText = "";
        String statTitle ="";
        if(grade.min_grade >=0) { statText += String.valueOf(grade.min_grade); statTitle += "Min"; }
        if(grade.max_grade >=0) { statText += "/"+grade.max_grade+"/"; statTitle += "/Max/"; }
        if(grade.coeff >=0) { statText += String.valueOf(grade.coeff); statTitle += "Coeff"; }
        viewHolder.TVgrade.setText(gradeText);


        viewHolder.TVgradeStats.setText(statText);
        viewHolder.TVgradeStatsTitle.setText(statTitle);

        return convertView;
    }

}
