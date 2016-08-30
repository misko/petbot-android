package com.petbot;

/**
 * Created by miskodzamba on 16-08-28.
 */
public class PBConnector {
    static {
        System.loadLibrary("PBConnector");
    }
    public static native String  stringFromJNI();
    public native byte[]  newByteArray();


    //pbsock* new_pbsock(int client_sock, SSL_CTX* ctx, int accept);
    //pbsock* connect_to_server_with_key(const char * hostname, int portno, SSL_CTX*ctx, const char * key);
    //pbsock* connect_to_server(const char * hostname, int portno, SSL_CTX* ctx);
    public String hostname = "";
    public int portno=0;
    public String key="";

    public native void connectToServerWithKey(String hostname, int portno, String key);

    public long ptr_pbs = 0; //the socket to server
    public long ptr_ctx = 0; //the ssl ctx reference pointer
}
