package com.petbot;

import android.util.Log;

/**
 * Created by miskodzamba on 16-08-28.
 */
public class PBConnector {
    static {
        System.loadLibrary("crypto");
        System.loadLibrary("ssl");
        System.loadLibrary("PBConnector");
    }
    public PBConnector (String hostname, int portno, String key) {
        Log.w("petbot", "Create PBConnector");
        Log.w("petbot", (new PBMsg()).toString());
        Log.w("petbot", (new PBMsg("Test")).toString());
        this.key=key;
        this.portno=portno;
        this.hostname=hostname;
        connectToServerWithKey(hostname,portno,key);

        //start up a read thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        sleep(1000);
                        PBMsg m = readPBMsg();
                        Log.w("petbot","GOT PBMSG FROM SERVER... should handle it here?" + m.toString());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }
    public static native String stringFromJNI();
    public native byte[] newByteArray();

    //pbsock* new_pbsock(int client_sock, SSL_CTX* ctx, int accept);
    //pbsock* connect_to_server_with_key(const char * hostname, int portno, SSL_CTX*ctx, const char * key);
    //pbsock* connect_to_server(const char * hostname, int portno, SSL_CTX* ctx);
    public String hostname = "";
    public int portno=0;
    public String key="";

    public native void connectToServerWithKey(String hostname, int portno, String key);
    public native PBMsg readPBMsg();
    public native void sendPBMsg(PBMsg m);
    public native void close();


    public long ptr_pbs = 0; //the socket to server
    public long ptr_ctx = 0; //the ssl ctx reference pointer
}
