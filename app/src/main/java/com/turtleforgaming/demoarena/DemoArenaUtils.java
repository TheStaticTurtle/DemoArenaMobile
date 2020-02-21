package com.turtleforgaming.demoarena;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ResultParseError extends Exception {
    ResultParseError(String errorMessage) {
        super(errorMessage);
    }
}

class LoginError extends Exception {
    LoginError(String errorMessage) {
        super(errorMessage);
    }
}

interface DemoarenaStage1Callback {
    void onSuccess(Bitmap captcha);
    void onFailure(Exception error);
}
interface DemoarenaStage2Callback {
    void onSuccess(String html);
    void onFailure(Exception error);
}

class DemoArenaUtils {
    private String DEMOARENA_STAGE1_COMMAND = "python -c 'import requests,re,base64,pickle;CAS = \"https://cas.univ-fcomte.fr/cas/login\";session = requests.Session();resp = session.get(CAS, verify=False, allow_redirects=True);lt = re.findall(r\"(LT-.+.-cas\\.univ-fcomte\\.fr)\",resp.text);assert len(lt)==1;print(\"OK\");session.post(CAS, data={\"username\":base64.b64decode(\"##INSERT-USER-HERE##\"), \"password\":base64.b64decode(\"##INSERT-PASS-HERE##\"), \"lt\":lt[0], \"_eventId\":\"submit\",\"execution\":\"e1s1\" } , verify=False, allow_redirects=False);resp = session.get(\"https://demoarena.iut-bm.univ-fcomte.fr/entree.php\", verify=False, allow_redirects=True);resp = session.get(\"https://demoarena.iut-bm.univ-fcomte.fr/securimage/securimage_show.php\", verify=False, allow_redirects=True);print({\"cookies\":session.cookies.get_dict(),\"image\":base64.b64encode(resp.content)});f = open(\".demoarena-cookies\", \"wb\");pickle.dump(session.cookies, f);f.close()' 2> .demoarena-logs";
    private String DEMOARENA_STAGE2_COMMAND = "python -c 'import requests,base64,pickle;f = open(\".demoarena-cookies\", \"rb\");session = requests.Session();session.cookies.update(pickle.load(f));f.close();print(base64.b64encode(session.post(\"https://demoarena.iut-bm.univ-fcomte.fr/traitement.php\", data={\"nip_VAL\":base64.b64decode(\"##INSERT-INE-HERE##\"), \"capt_Code\":base64.b64decode(\"##INSERT-CAPTCHA-HERE##\")}, verify=False, allow_redirects=True).text.encode(\"UTF-8\")))' 2>> .demoarena-logs";
    private String DEMOARENA_STAGE3_COMMAND = "python -c 'import requests,base64,pickle;f = open(\".demoarena-cookies\", \"rb\");session = requests.Session();session.cookies.update(pickle.load(f));f.close();print(base64.b64encode(session.post(\"https://demoarena.iut-bm.univ-fcomte.fr/traitement.php\", data={\"semestre\":base64.b64decode(\"##INSERT-ID-HERE##\")}, verify=False, allow_redirects=True).text.encode(\"UTF-8\")))' 2>> .demoarena-logs";

    private String result_stage1 = "";
    private String result_stage2 = "";
    private SshManager manager;

    Bitmap latestCaptcha;
    private String pageHtml = "";

    DemoArenaUtils(SshManager manager) {
        this.manager = manager;
        /*if(!this.manager.isInit()) {
            this.manager.init();
        }*/
    }

    private void setStage1Result(String result) {
        this.result_stage1 = result;
    }
    private void setStage2Result(String result) {
        this.result_stage2 = result;
    }

    @SuppressWarnings("all")
    boolean validateStage1() throws ResultParseError,LoginError,JSONException{
        if(this.result_stage1.contains("OK")) {
            String[] rawData = this.result_stage1.split("\n");
            if(rawData.length == 2) {
                JSONObject jsonData = new JSONObject(rawData[1]);
                JSONObject cookies = jsonData.getJSONObject("cookies");
                final String base64image = jsonData.getString("image");

                if(cookies.has("AGIMUS")) {
                    final String cook = cookies.toString();
                    byte[] decodedString = Base64.decode(base64image, Base64.DEFAULT);
                    latestCaptcha = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                } else {
                    throw new LoginError("Erreur, cookie AGIMUS non present: Mot de passe ou utilisateur incorrect");
                }
            } else {
                throw new ResultParseError("Erreur, la commande a retourner plus que 2 lignes (Erreur de l'application voir .demoarena.log)");
            }
        } else {
            throw new ResultParseError("Erreur, impossible de lire le tag LT. (Deux problemes possible: Seveur CAS down ou erreur de l'application voir .demoarena-log)");
        }
        return true;
    }

    @SuppressWarnings("all")
    boolean validateStage2() throws LoginError {
        final byte[] decodedBytes = Base64.decode(this.result_stage2, Base64.DEFAULT);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        if(decodedString.contains("La valeur du captcha")) {
            throw new LoginError("Capcha invalide.");
        }
        if(decodedString.contains("Utilisateur non authenti")) {
            throw new LoginError("Erreur impossible, relancer l'application.");
        }
        if(decodedString.contains("Num") && decodedString.contains("tudiant(e) inconnu")) {
            throw new LoginError("Erreur, numero etudiant incorrect..");
        }

        this.pageHtml = decodedString;
        return true;
    }

    @SuppressWarnings("all")
    boolean validateStage3() throws LoginError {
        final byte[] decodedBytes = Base64.decode(this.result_stage2, Base64.DEFAULT);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

        if(decodedString.contains("La valeur du captcha")) {
            throw new LoginError("Capcha invalide.");
        }
        if(decodedString.contains("Utilisateur non authenti")) {
            throw new LoginError("Erreur impossible, relancer l'application.");
        }
        if(decodedString.contains("Num") && decodedString.contains("tudiant(e) inconnu")) {
            throw new LoginError("Erreur, numero etudiant incorrect..");
        }

        this.pageHtml = decodedString;
        return true;
    }

    void stage1(String username, String password, final DemoarenaStage1Callback cb) {

        String command = DEMOARENA_STAGE1_COMMAND;
        command = command.replace("##INSERT-USER-HERE##", Base64.encodeToString(username.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        command = command.replace("##INSERT-PASS-HERE##", Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        command = command.replace("\n","").replace("\r","");

        this.manager.execute(command, new SshResultCallback() {
            @Override
            public void onSuccess(String result) {
                setStage1Result(result);
                try {
                    validateStage1();
                    cb.onSuccess(latestCaptcha);
                } catch (ResultParseError|LoginError|JSONException e) {
                    cb.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                cb.onFailure(e);
            }
        });
    }

    void stage2(String ine, String captcha, final DemoarenaStage2Callback cb) {
        String command = DEMOARENA_STAGE2_COMMAND;
        command = command.replace("##INSERT-CAPTCHA-HERE##", Base64.encodeToString(captcha.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        command = command.replace("##INSERT-INE-HERE##", Base64.encodeToString(ine.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        command = command.replace("\n","").replace("\r","");

        this.manager.execute(command, new SshResultCallback() {
            @Override
            public void onSuccess(String result) {
                setStage2Result(result);
                try {
                    validateStage2();
                    cb.onSuccess(pageHtml);
                } catch (LoginError e) {
                    cb.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                cb.onFailure(e);
            }
        });
    }

    void stage3(String semestreID, final DemoarenaStage2Callback cb) {
        String command = DEMOARENA_STAGE3_COMMAND;
        command = command.replace("##INSERT-ID-HERE##", Base64.encodeToString(semestreID.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        command = command.replace("\n","").replace("\r","");

        this.manager.execute(command, new SshResultCallback() {
            @Override
            public void onSuccess(String result) {
                setStage2Result(result);
                try {
                    validateStage3();
                    cb.onSuccess(pageHtml);
                } catch (LoginError e) {
                    cb.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                cb.onFailure(e);
            }
        });
    }

    @SuppressWarnings("all")
    User parseCurrentPage() {
        Document doc = Jsoup.parse(pageHtml);

        User user = new User();

        Element nameH1 = doc.select("body > div.bulletin > table > tbody > tr > td:nth-child(2) > table > tbody > tr:nth-child(1) > td > h1").first();
        Element formatinoH1 = doc.select("body > div.bulletin > table > tbody > tr > td:nth-child(2) > table > tbody > tr:nth-child(2) > td > h1 > b").first();
        Element semesterSelector = doc.select("body > form > fieldset > p > select").first();

        for(Element el : semesterSelector.children()) {
            Semester sem = new Semester();
            sem.id = el.attr("value");
            sem.name = el.text();
            user.semesters.add(sem);
        }
        user.name = nameH1.text();
        user.formation = formatinoH1.text();

        Element absanceTable = doc.select("#absences").first();
        if(absanceTable != null) {
            int i = 0;
            for(Element el : absanceTable.getElementsByTag("tr")) {
                if(i > 0) {
                    Absence a = new Absence();
                    a.from = el.getElementsByTag("td").get(0).text();
                    a.to = el.getElementsByTag("td").get(1).text();
                    a.justified = el.getElementsByTag("td").get(2).text().equals("Oui");
                    a.cause = el.getElementsByTag("td").get(3).text();
                    user.semesters.get(0).absences.add(a);
                }
                i++;
            }
        }

        user.semesters.get(0).done = doc.html().contains("Les informations contenues dans ce tableau sont définitives");

        Element gradesTable = doc.select(".notes_bulletin").first();
        if(gradesTable != null) {
            boolean foundFirstUE = false;
            UE currentUE = new UE();
            Course currentCourse = new Course();

            int i=0;
            for(Element el : gradesTable.getElementsByTag("tr")) {
                if(i>0 && !user.semesters.get(0).done || i>1 && user.semesters.get(0).done ) {
                    if (el.hasClass("notes_bulletin_row_ue")) {

                        currentUE = new UE();

                        Pattern pattern = Pattern.compile("</span>(.*)<br>(.*)</td>");
                        Matcher matcher = pattern.matcher(el.html());
                        if (matcher.find()) {
                            currentUE.id = matcher.group(1);
                            currentUE.name = matcher.group(2);
                        } else {
                            currentUE.name = el.text();
                        }
                        currentUE.grade = Double.parseDouble(el.child(2).text());
                        currentUE.coeff = Double.parseDouble(el.child(4).text());

                        Pattern pattern2 = Pattern.compile("(\\d+.\\d+)/(\\d+.\\d+)");
                        Matcher matcher2 = pattern2.matcher(el.html());
                        if (matcher2.find()) {
                            currentUE.min_grade = Double.parseDouble(matcher2.group(1));
                            currentUE.max_grade = Double.parseDouble(matcher2.group(2));
                        }

                        user.semesters.get(0).ues.add(currentUE);


                    } else if (el.hasClass("toggle4") || user.semesters.get(0).done) {
                        currentCourse = new Course();
                        currentCourse.id = el.child(1).text();
                        currentCourse.name = el.child(2).text();
                        currentCourse.grade = Double.parseDouble(el.child(4).text());
                        currentCourse.coeff = Double.parseDouble(el.child(6).text());

                        Pattern pattern = Pattern.compile("(\\d+.\\d+)/(\\d+.\\d+)");
                        Matcher matcher = pattern.matcher(el.html());
                        if (matcher.find()) {
                            currentCourse.min_grade = Double.parseDouble(matcher.group(1));
                            currentCourse.max_grade = Double.parseDouble(matcher.group(2));
                        }

                        currentUE.courses.add(currentCourse);

                    } else if (!user.semesters.get(0).done) {
                        Grade grade = new Grade();


                        currentCourse.grades.add(grade);
                    }
                }
                i++;
            }

            if(foundFirstUE) {
                user.semesters.get(0).ues.add(currentUE);
            }

            for(UE ue : user.semesters.get(0).ues) {
                if(ue.name.toLowerCase().contains("bonus")) {
                    double totalNotes = 0;
                    double totalCoeff = 0;
                    for(Course cr : ue.courses) {
                        if(cr.grade > 0) {
                            totalNotes += cr.grade * cr.coeff;
                            totalCoeff += cr.coeff;
                        }
                    }
                    Grade moy = new Grade();
                    moy.id = "BONUS";
                    moy.name = ue.id;
                    moy.coeff = -1;
                    moy.grade = Math.round( (totalNotes / totalCoeff)*100.0 ) /100.0;
                    moy.max_grade = -1;
                    moy.min_grade = -1;
                    moy.outof = -1;
                    moy.type = "BONUS";
                    moy.showId = false;
                    user.semesters.get(0).uesMoy.add(moy);

                } else {
                    double totalNotes = 0;
                    double totalCoeff = 0;
                    for(Course cr : ue.courses) {
                        if(cr.grade > 0) {
                            totalNotes += cr.grade * cr.coeff;
                            totalCoeff += cr.coeff;
                        }
                    }
                    Grade moy = new Grade();
                    moy.id = "MOY";
                    moy.name = ue.name;
                    moy.coeff = totalCoeff;
                    moy.grade = Math.round( (totalNotes / totalCoeff)*100.0 ) /100.0;
                    moy.max_grade = -1;
                    moy.min_grade = -1;
                    moy.type = "MOY";
                    moy.showId = false;
                    user.semesters.get(0).uesMoy.add(moy);
                }
            }

            double totalNotes = 0.0;
            double totalCoeff = 0.0;
            double noteOffset = 0.0;
            for(Grade gr : user.semesters.get(0).uesMoy) {
                if(gr.type.equals("MOY")) {
                    totalNotes += gr.grade * gr.coeff;
                    totalCoeff += gr.coeff;
                }
                if(gr.type.equals("BONUS")) {
                    noteOffset += gr.grade;
                }
            }
            Grade g = new Grade();
            g.type = "MOYGEN";
            g.id = "MOYGEN";
            g.name = "Moyenne generale";
            g.coeff = -1;
            g.max_grade = -1;
            g.min_grade = -1;
            g.grade =  Math.round( (totalNotes / totalCoeff)*100.0 ) /100.0;
            g.grade += noteOffset;
            g.showId = false;
            user.semesters.get(0).moyGen = g;
        }

        return user;
    }

    class User {
        String name;
        String formation;
        ArrayList<Semester> semesters = new ArrayList<>();
    }

    class Semester {
        String id;
        String name;
        ArrayList<UE> ues = new ArrayList<>();
        ArrayList<Grade> uesMoy = new ArrayList<>();
        Grade moyGen;
        ArrayList<Absence> absences = new ArrayList<>();
        boolean done = false;
    }

    class Grade {
        String id;
        String name;
        double grade = 0;
        double outof = 20;
        double min_grade = 0;
        double max_grade = 0;
        double coeff = 0;
        String type = "GRADE";
        boolean showId = true;
    }

    class UE extends Grade {
        ArrayList<Course> courses = new ArrayList<>();

        UE() {
            this.type = "UE";
        }
    }

    class Course extends Grade {
        ArrayList<Grade> grades = new ArrayList<>();
        Course() {
            this.type = "COURSE";
        }
    }


    class Absence {
        String from;
        String to;
        boolean justified;
        String cause;
    }
}
