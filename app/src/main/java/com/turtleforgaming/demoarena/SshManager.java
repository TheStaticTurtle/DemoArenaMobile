package com.turtleforgaming.demoarena;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.io.CharStreams;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetSocketAddress;


interface SshResultCallback {
    void onSuccess(String result);
    void onFailure(Exception error);
}

interface SshConnectionResult {
    void onSuccess();
    void onFailure(Exception error);
}

class SshCommand {
    String command ="";
    SshResultCallback callback;
    Session session;

    public SshCommand(Session session, String command, SshResultCallback callback) {
        this.session = session;
        this.command = command;
        this.callback = callback;
    }
    public SshCommand(Session session, String command) {
        this.session = session;
        this.command = command;
        this.callback = null;
    }
}

class Networkdddress {
    String hostname ="";
    int port;

    public Networkdddress(String host, int port) {
        this.hostname = host;
        this.port = port;
    }
}


class SshExecutor extends AsyncTask<SshCommand, Void, Void> {
    @Override
    protected Void doInBackground(SshCommand... params) {
        try {
            ChannelExec channel = (ChannelExec)params[0].session.openChannel("exec");
            channel.setCommand(params[0].command);

            boolean hasCallBack = params[0].callback != null;
            String result = "";

            channel.connect();
            if(hasCallBack) {
                result =  CharStreams.toString(new InputStreamReader(channel.getInputStream()));
            }
            channel.disconnect();

            if(hasCallBack) {
                params[0].callback.onSuccess(result);
            }
        } catch (Exception e) {
            Log.e("SshManager","Failed to execute: "+e.toString());
            if(params[0].callback != null) {
                params[0].callback.onFailure(e);
            }
        }
        return null;
    }
}

class SshManager {
    private Networkdddress host;
    private String user = "";
    private String pass = "";
    private Session session = null;
    private SshResultCallback callback;

    public SshManager(Networkdddress host, String user, String pass) {
        this.host = host;
        this.user = user;
        this.pass = pass;
    }

    protected void setCredentials(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }
    @SuppressWarnings("all")
    protected void init(final SshConnectionResult cb) {
        try {
            JSch jsch = new JSch();
            this.session = jsch.getSession(this.user, this.host.hostname, this.host.port);
            this.session.setPassword(this.pass);
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.setTimeout(10000);


            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        session.connect();
                        cb.onSuccess();
                    } catch (Exception e) {
                        Log.e("SshManager","Failed to initialize SshSession: "+e.toString());
                        cb.onFailure(e);
                    }
                    return null;
                }
            }.execute();
        } catch (JSchException e) {
            Log.e("SshManager","Failed to initialize SshSession: "+e.toString());
            cb.onFailure(e);
        }
    }

    protected boolean isInit() {
        return this.session != null && this.session.isConnected();
    }

    public void quit() {
        this.session.disconnect();
    }

    public void execute(final String command) {
        SshExecutor task = new SshExecutor();
        task.execute(new SshCommand(this.session,command));
    }

    public void execute(final String command, final SshResultCallback cb) {
        SshExecutor task = new SshExecutor();
        task.execute(new SshCommand(this.session,command,cb));
    }
}