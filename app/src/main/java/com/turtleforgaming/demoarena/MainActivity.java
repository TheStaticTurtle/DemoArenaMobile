package com.turtleforgaming.demoarena;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;
    GifAnimatedView gifImageView;

    private TextView userView;
    private TextView passView;
    private TextView ineView;
    private Switch checkView;
    private Button buttonConnect;
    private Button buttonCaptcha;

    private SshManager manager;
    private DemoArenaUtils demoArenaUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.textView_status);

        userView = findViewById(R.id.editTextLogin);
        passView = findViewById(R.id.editTextPassword);
        ineView = findViewById(R.id.editTextINE);
        checkView = findViewById(R.id.savePassword);

        View.OnKeyListener listner = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                updatePreference(); return false;
            }
        };

        userView .setOnKeyListener(listner);
        passView .setOnKeyListener(listner);
        ineView  .setOnKeyListener(listner);
        checkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePreference();
            }
        });

        gifImageView = findViewById(R.id.GifImageView);
        gifImageView.setGifImageResource(R.drawable.info_iut_still);

        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        buttonCaptcha = findViewById(R.id.buttonValidateCacha);

        /*Button buttonDisonnect = findViewById(R.id.button2);
        buttonDisonnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disconnect();
                Log.wtf("TAG", "DECO");
            }
        });*/

        loadPreferences();
        manager = new SshManager(new Networkdddress("gate-info.iut-bm.univ-fcomte.fr",22),userView.getText().toString(),passView.getText().toString());
        demoArenaUtils = new DemoArenaUtils(manager);
    }

    private void updatePreference() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Config", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();

        editor.putString("username"   , userView.getText().toString());
        editor.putString("ine"        , ineView .getText().toString());
        editor.putBoolean("savePasswd", checkView.isChecked());

        if(checkView.isChecked()) {
            editor.putString("password", passView.getText().toString());
        } else {
            editor.putString("password", "");
        }

        editor.apply();
    }
    private void loadPreferences() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Config", 0); // 0 - for private mode
        checkView.setChecked(pref.getBoolean("savePasswd",true));
        userView.setText(pref.getString("username", ""));
        passView.setText(pref.getString("password", "5"));
        ineView.setText(pref.getString("ine", ""));
    }

    private void connect() {
        buttonConnect.setEnabled(false);
        gifImageView.setGifImageResource(R.drawable.info_iut);
        demoArenaUtils.stage1(userView.getText().toString(),passView.getText().toString(),new DemoarenaStage1Callback() {
            @Override
            public void onSuccess(Bitmap captcha) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);
                        ImageView cp = findViewById(R.id.imageView);
                        cp.setImageBitmap(demoArenaUtils.latestCaptcha);

                        findViewById(R.id.tableRowCatcha).setVisibility(View.VISIBLE);
                        findViewById(R.id.tableRowCatchaBtn).setVisibility(View.VISIBLE);
                        findViewById(R.id.tableRowConnect).setVisibility(View.GONE);

                        findViewById(R.id.buttonValidateCacha).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText captcha = findViewById(R.id.editTextCaptcha);
                                validateCaptcha(captcha.getText().toString());
                            }
                        });
                        tvStatus.setText(getText(R.string.stage2_captcha));
                    }
                });

            }

            @Override
            public void onFailure(final Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonConnect.setEnabled(true);
                        buttonCaptcha.setEnabled(true);
                        tvStatus.setText("Erreur a l'etape 1:\n" + error.toString());
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);
                    }
                });
            }
        });
    }

    @SuppressWarnings("all")
    private void validateCaptcha(String captcha) {
        buttonCaptcha.setEnabled(false);
        gifImageView.setGifImageResource(R.drawable.info_iut);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("Config", 0); // 0 - for private mode

        demoArenaUtils.stage2(pref.getString("ine", ""), captcha, new DemoarenaStage2Callback() {
            @Override
            public void onSuccess(String html) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);
                        /*
                        setContentView(R.layout.layout_demoarena);
                        WebView tmp = findViewById(R.id.tmp);
                        tmp.getSettings().setJavaScriptEnabled(true);
                        tmp.loadDataWithBaseURL("", demoArenaUtils.pageHtml, "text/html", "UTF-8", "");
                        */
                        setupViewer();
                    }
                });
            }

            @Override
            public void onFailure(final Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                        findViewById(R.id.tableRowCatcha).setVisibility(View.GONE);
                        findViewById(R.id.tableRowCatchaBtn).setVisibility(View.GONE);
                        findViewById(R.id.tableRowConnect).setVisibility(View.VISIBLE);
                        buttonConnect.setEnabled(true);
                        buttonCaptcha.setEnabled(true);
                        tvStatus.setText("Erreur a l'etape 2:" + error.getMessage());
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);

                    }
                });
            }
        });
    }

    private void setupViewer() {
        setContentView(R.layout.layout_demoarena);
        final TabLayout tabLayout = findViewById(R.id.TabLayout);
        TextView textViewUserName = findViewById(R.id.textViewUserName);
        TextView textViewFormationName = findViewById(R.id.textViewFormationName);
        Spinner spinnerPeriods = findViewById(R.id.spinnerPeriods);
        final ViewFlipper vf = (ViewFlipper)findViewById(R.id.layoutFlipper);


        DemoArenaUtils.User user = demoArenaUtils.parseCurrentPage();

        textViewUserName.setText(user.name);
        textViewFormationName.setText(user.formation);

        final List<DemoArenaUtils.Semester> sems = user.semesters;
        SemesterAdapter semesterAdapter = new SemesterAdapter(MainActivity.this, sems);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriods.setAdapter(semesterAdapter);


        gifImageView = findViewById(R.id.GifImageViewLoading);
        gifImageView.setGifImageResource(R.drawable.info_iut);

        spinnerPeriods.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                vf.setDisplayedChild(2);
                DemoArenaUtils.Semester sem = ((SemesterAdapter)parent.getAdapter()).getItem(position);
                demoArenaUtils.stage3(sem.id, new DemoarenaStage2Callback() {
                    @Override
                    public void onSuccess(String html) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DemoArenaUtils.User user = demoArenaUtils.parseCurrentPage();
                                if(tabLayout.getSelectedTabPosition() == 0) {
                                    if(user.semesters.get(0).ues.size() >0) {
                                        vf.setDisplayedChild(0);
                                    } else {
                                        vf.setDisplayedChild(3);
                                    }
                                }
                                if(tabLayout.getSelectedTabPosition() == 1) {
                                    if(user.semesters.get(0).absences.size() >0) {
                                        vf.setDisplayedChild(1);
                                    } else {
                                        vf.setDisplayedChild(3);
                                    }
                                }
                                updateViews();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        Toast.makeText(getApplicationContext(),"Erreur: "+error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                DemoArenaUtils.User user = demoArenaUtils.parseCurrentPage();
                if(tab.getPosition() == 0) {
                    if(user.semesters.get(0).ues.size() >0) {
                        vf.setDisplayedChild(0);
                    } else {
                        vf.setDisplayedChild(3);
                    }
                }
                if(tab.getPosition() == 1) {
                    if(user.semesters.get(0).absences.size() >0) {
                        vf.setDisplayedChild(1);
                    } else {
                        vf.setDisplayedChild(3);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        updateViews();
    }


    void updateViews() {
        ListView absenceList = findViewById(R.id.listAbsences);
        ListView gradesList = findViewById(R.id.listGrades);

        DemoArenaUtils.User user = demoArenaUtils.parseCurrentPage();

        List<DemoArenaUtils.Absence> abs =  user.semesters.get(0).absences;
        AbsenceAdapter absAdapter = new AbsenceAdapter(MainActivity.this, abs);
        absenceList.setAdapter(absAdapter);


        List<DemoArenaUtils.Grade> grades = new ArrayList<>();

        if(user.semesters.get(0).moyGen != null) {
            grades.add(user.semesters.get(0).moyGen);
        }

        for(DemoArenaUtils.Grade uemoy : user.semesters.get(0).uesMoy) {
            grades.add(uemoy);
        }

        for(DemoArenaUtils.UE ue : user.semesters.get(0).ues) {
            grades.add(ue);
            for(DemoArenaUtils.Course course : ue.courses) {
                grades.add(course);
                for(DemoArenaUtils.Grade grade: course.grades) {
                    grades.add(grade);
                }
            }
        }
        GradeAdapter grdAdapter = new GradeAdapter(MainActivity.this, grades);
        gradesList.setAdapter(grdAdapter);
    }
}
