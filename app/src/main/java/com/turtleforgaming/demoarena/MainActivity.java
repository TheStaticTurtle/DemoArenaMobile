package com.turtleforgaming.demoarena;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.ContextThemeWrapper;
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

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.jcraft.jsch.JSchException;

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
                updatePreference(0); return false;
            }
        };

        userView .setOnKeyListener(listner);
        passView .setOnKeyListener(listner);
        ineView  .setOnKeyListener(listner);
        checkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePreference(1);
            }
        });

        gifImageView = findViewById(R.id.GifImageView);
        gifImageView.setGifImageResource(R.drawable.info_iut_still);

        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectGateInfo();
            }
        });
        buttonCaptcha = findViewById(R.id.buttonValidateCacha);

        InputFilter enterFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start;i < end;i++) {
                    if (Character.toString(source.charAt(i)).equals("\n")) {
                        return "";
                    }
                }
                return null;
            }
        };

        EditText captcha = findViewById(R.id.editTextCaptcha);
        captcha.setFilters(new InputFilter[] { enterFilter, new InputFilter.LengthFilter(6) });

        loadPreferences();
        manager = new SshManager(new Networkdddress("gate-info.iut-bm.univ-fcomte.fr",22),userView.getText().toString(),passView.getText().toString());
        demoArenaUtils = new DemoArenaUtils(manager);
    }

    private String prettifyErrors(Exception e) {
        String et = e.toString();
        String text = et.split(":")[et.split(":").length-1];

        if(et.contains("Connection reset")) {
            return getString(R.string.pretty_errors_connection_reset,text);
        } else if (et.contains("Software caused connection abort")) {
            return getString(R.string.pretty_errors_soft_abort,text);
        } else if (et.contains("Auth fail")) {
            return getString(R.string.pretty_errors_auth_fail);
        } else if (et.contains(getString(R.string.stage2_invalidCaptcha))) {
            return getString(R.string.stage2_invalidCaptcha);
        }
        return getString(R.string.pretty_errors_unknown, e);
    }




    protected void popup(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme));

        builder.setMessage(message)
                .setTitle(title)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updatePreference(int saveMode) {
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

        if(saveMode == 1) {
            editor.commit();
        } else {
            editor.apply();
        }
    }
    private void loadPreferences() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Config", 0); // 0 - for private mode
        checkView.setChecked(pref.getBoolean("savePasswd",true));
        userView.setText(pref.getString("username", ""));
        passView.setText(pref.getString("password", ""));
        ineView.setText(pref.getString("ine", ""));
    }

    private void connectGateInfo() {
        updatePreference(1);
        buttonConnect.setEnabled(false);
        gifImageView.setGifImageResource(R.drawable.info_iut);

        if(!manager.isInit()) {
            manager.setCredentials(userView.getText().toString(),passView.getText().toString());
            manager.init(new SshConnectionResult() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(getResources().getString(R.string.login_gateinfo_connected));
                            connect();
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
                            tvStatus.setText(getResources().getString(R.string.login_gateinfo_error, prettifyErrors(error)));
                            gifImageView.setGifImageResource(R.drawable.info_iut_still);
                            popup(getString(R.string.dialog_error),prettifyErrors(error));
                        }
                    });

                }
            });
        } else {
            connect();
        }
    }
    private void connect() {

        final ImageView cp = findViewById(R.id.imageView);
        cp.setImageDrawable(getDrawable(R.drawable.loading));

        demoArenaUtils.stage1(userView.getText().toString(),passView.getText().toString(),new DemoarenaStage1Callback() {
            @Override
            public void onSuccess(Bitmap captcha) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);

                        cp.setImageBitmap(demoArenaUtils.latestCaptcha);
                        buttonCaptcha.setEnabled(true);

                        findViewById(R.id.tableRowCatcha).setVisibility(View.VISIBLE);
                        findViewById(R.id.tableRowCatchaBtn).setVisibility(View.VISIBLE);
                        findViewById(R.id.tableRowConnect).setVisibility(View.GONE);

                        findViewById(R.id.buttonValidateCacha).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText captcha = findViewById(R.id.editTextCaptcha);

                                if (captcha.getText().length() == 6) {
                                    validateCaptcha(captcha.getText().toString());
                                } else {
                                    Toast.makeText(getApplicationContext(),getText(R.string.stage2_captcha), Toast.LENGTH_LONG).show();
                                }
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
                        tvStatus.setText(getResources().getString(R.string.stage1_error, prettifyErrors(error)));
                        gifImageView.setGifImageResource(R.drawable.info_iut_still);
                        popup(getString(R.string.dialog_error),prettifyErrors(error));
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
                        tvStatus.setText(getString(R.string.stage2_error,prettifyErrors(error)));
                        popup(getString(R.string.dialog_error),prettifyErrors(error));
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
        final ViewFlipper vf = findViewById(R.id.layoutFlipper);


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
                        Toast.makeText(getApplicationContext(),prettifyErrors(error), Toast.LENGTH_LONG).show();
                        popup(getString(R.string.dialog_error),prettifyErrors(error));
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

        grades.addAll(user.semesters.get(0).uesMoy);

        for(DemoArenaUtils.UE ue : user.semesters.get(0).ues) {
            grades.add(ue);
            for(DemoArenaUtils.Course course : ue.courses) {
                grades.add(course);
                grades.addAll(course.grades);
            }
        }
        GradeAdapter grdAdapter = new GradeAdapter(MainActivity.this, grades);
        gradesList.setAdapter(grdAdapter);

    }
}
