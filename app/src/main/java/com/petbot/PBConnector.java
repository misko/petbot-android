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
        initGlib();
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
		PBMsg m = new PBMsg(url, PBMsg.PBMSG_SOUND | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
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
    public native void startNiceThread(int s);
    public native void iceRequest();
    public native void iceNegotiate(PBMsg m);
    public native void initGlib();


    public long ptr_pbs = 0; //the socket to server
    public long ptr_ctx = 0; //the ssl ctx reference pointer
    public long ptr_ice_thread_pipes_from_child = 0;
    public long ptr_ice_thread_pipes_to_child = 0;
    public int streamer_id=0;

    public long ptr_agent=0;
    public int stream_id=0;
    public long ptr_mainloop=0;
}
