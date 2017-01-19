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

    public PBConnector (String hostname, int portno, String key, String stun_server, String stun_port, String stun_username , String stun_password) {
        Log.w("petbot", "Create PBConnector");
        Log.w("petbot", (new PBMsg()).toString());
        Log.w("petbot", (new PBMsg("Test")).toString());

        if (stun_server!=null && stun_port!=null) {
            setStun(stun_server,stun_port,stun_username,stun_password);
        }

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
        String command = "PLAYURL " + url;
		PBMsg m = new PBMsg(command, PBMsg.PBMSG_SOUND | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
		sendPBMsg(m);
	}

    public void camera_fx_up() {
        PBMsg m = new PBMsg("adjust_fx 1", PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void camera_fx_down() {
        PBMsg m = new PBMsg("adjust_fx -1", PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void camera_exp_up() {
        PBMsg m = new PBMsg("adjust_exp 1", PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void camera_exp_down() {
        PBMsg m = new PBMsg("adjust_exp -1", PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_REQUEST | PBMsg.PBMSG_STRING);
        sendPBMsg(m);
    }

    public void close () {
        Log.w("petbot", "ANDROID - JAVA PBCONNECTOR - CLOSE ");
        nativeClose();
    }

	public void update() {
		PBMsg m = new PBMsg("UPDATE updates@updates.petbot.ca:/", PBMsg.PBMSG_UPDATE | PBMsg.PBMSG_STRING | PBMsg.PBMSG_REQUEST);
		sendPBMsg(m);
	}

	public void cancelUpdate() {
		PBMsg m = new PBMsg("STOP", PBMsg.PBMSG_UPDATE | PBMsg.PBMSG_STRING | PBMsg.PBMSG_REQUEST);
		sendPBMsg(m);
	}


    public String hostname = "";
    public int portno=0;
    public String key="";

    public native void setStun(String server, String port, String username, String password);
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
