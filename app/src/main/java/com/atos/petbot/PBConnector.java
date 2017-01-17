package com.atos.petbot;

import android.util.Log;

/**
 * Created by miskodzamba on 16-08-28.
 */
public class PBConnector {
    static {
        Log.d("petbot", "Try to make pbconnector x2");
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
        init();
    }

	public void getSettings(){
		PBMsg m = new PBMsg("all", PBMsg.PBMSG_CONFIG_GET | PBMsg.PBMSG_STRING | PBMsg.PBMSG_REQUEST);
		sendPBMsg(m);
	}

	public void set(String property, String value){
        Log.w("petbot","SET"+property + "\t" + value);
		PBMsg m = new PBMsg(property + "\t" + value, PBMsg.PBMSG_CONFIG_SET | PBMsg.PBMSG_STRING | PBMsg.PBMSG_REQUEST);
		sendPBMsg(m);
	}

    public void getUptime() {
        PBMsg m = new PBMsg("UPTIME", PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void takeSelfie() {
        PBMsg m = new PBMsg("selfie", PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void sendCookie() {
        PBMsg m = new PBMsg("cookie",PBMsg.PBMSG_COOKIE | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

	public void playSound(String url) {
        String command = "PLAYURL "+url;
		PBMsg m = new PBMsg(command, PBMsg.PBMSG_SOUND | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
		sendPBMsg(m);
	}

    public void camera_fx_up() {
        String command = "adjust_fx 1";
        PBMsg m = new PBMsg(command, PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }
    public void camera_fx_down() {
        String command = "adjust_fx -1";
        PBMsg m = new PBMsg(command, PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }
    public void camera_exp_up() {
        String command = "adjust_exp 1";
        PBMsg m = new PBMsg(command, PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }
    public void camera_exp_down() {
        String command = "adjust_exp -1";
        PBMsg m = new PBMsg(command, PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void close () {
        Log.w("petbot", "ANDROID - JAVA PBCONNECTOR - CLOSE ");
        nativeClose();
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
    public native void nativeClose();
    //public native void startNiceThread(int s);
    public native void iceRequest();
    public native void makeIceRequest();
    public native void iceNegotiate(PBMsg m);
    public native void init();


    public long ptr_pbnio = 0; //the socket to server
    public long ptr_pbs = 0; //the socket to server
    public long ptr_ctx = 0; //the ssl ctx reference pointer
    public long ptr_ice_thread_pipes_from_child = 0;
    public long ptr_ice_thread_pipes_to_child = 0;
    public int streamer_id=0;

    public long ptr_agent=0;
    public int stream_id=0;
    public long ptr_mainloop=0;
}
