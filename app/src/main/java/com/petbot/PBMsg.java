
package com.petbot;
import java.nio.ByteBuffer;


/**
 * Created by miskodzamba on 16-08-30.
 */
public class PBMsg {
    public static int PBMSG_FAIL = (1<<0);
    public static int PBMSG_SUCCESS = (1<<1);
    public static int PBMSG_BUSY = (1<<2);
    public static int PBMSG_REQUEST = (1<<3);
    public static int PBMSG_RESPONSE = (1<<4);
    public static int PBMSG_EVENT = (1<<5);
    public static int PBMSG_VIDEO = (1<<6);
    public static int PBMSG_ICE = (1<<7);
    public static int PBMSG_COOKIE = (1<<8);
    public static int PBMSG_SOUND = (1<<9);
    public static int PBMSG_WIFI = (1<<10);
    public static int PBMSG_LED = (1<<11);
    public static int PBMSG_CLIENT = (1<<12);
    public static int PBMSG_SERVER = (1<<13);
    public static int PBMSG_ALL = (1<<14);
    public static int PBMSG_CONFIG_SET = (1<<15);
    public static int PBMSG_CONFIG_GET = (1<<16);
    public static int PBMSG_CONNECTED = (1<<17);
    public static int PBMSG_DISCONNECTED = (1<<18);
    public static int PBMSG_STRING = (1<<19);
    public static int PBMSG_PTR = (1<<20);
    public static int PBMSG_BIN = (1<<21);
    public static int PBMSG_FILE = (1<<22);
    public static int PBMSG_KEEP_ALIVE = (1<<23);
    /*
void free_pbmsg(pbmsg * m);
pbmsg * new_pbmsg();
pbmsg * new_pbmsg_from_str_wtype(const char * s, int type);
pbmsg * new_pbmsg_from_str(const char *s);
pbmsg * new_pbmsg_from_file(const char *s);
pbmsg * new_pbmsg_from_ptr(void * x );
pbmsg * new_pbmsg_from_ptr_and_int(void *x , int z);
int pbmsg_to_file(pbmsg * m, const char * fn);
//Send / recv pbmsg over pbsock
pbmsg * recv_pbmsg(pbsock * pbs);
size_t send_pbmsg(pbsock *, pbmsg *m);
pbmsg * recv_fd_pbmsg(int fd);
pbmsg * recv_all_fd_pbmsg(int fd,int read_all);
size_t send_fd_pbmsg(int fd, pbmsg *m);
*/

    public PBMsg() {
        //the base constructor
        pbmsg_len=0;
        pbmsg_type=0;
        pbmsg_from=0;
    }

    public PBMsg (String s) {
        pbmsg_len=s.length()+1; //null terminator
        pbmsg = new byte[pbmsg_len];
        //TODO move to array copy?
        byte[] s_ray = s.getBytes();
        for (int i=0; i<s.length(); i++) {
            pbmsg[i]=s_ray[i];
        }
        pbmsg[s.length()]=0; // 'null terminator?
        pbmsg_type=PBMSG_STRING;
        pbmsg_from=0;
    }

    public PBMsg(String s, int type) {
        this(s);
        pbmsg_type=type;
    }

    public PBMsg(int x1) {
        pbmsg_from=0;
        pbmsg_len=4;
        pbmsg_type=PBMSG_PTR;
        pbmsg = ByteBuffer.allocate(4).putInt(x1).array();
    }
    public PBMsg(int x1, int x2) {
        pbmsg_from=0;
        pbmsg_len=8;
        pbmsg_type=PBMSG_PTR;
        pbmsg = new byte[8];
        byte[] b1 = ByteBuffer.allocate(4).putInt(x1).array();
        byte[] b2 = ByteBuffer.allocate(4).putInt(x2).array();
        for (int i=0; i<4; i++) {
            pbmsg[i]=b1[i];
            pbmsg[i+4]=b2[i];
        }
        pbmsg_from=0;
    }

    public PBMsg pbmsg_to_file(String s) {
        //TODO IMPLEMENT!
        //load a file here...
        assert(1==0);
        return this;
    }

    public String toString() {
        if ((pbmsg_type&PBMSG_STRING)==0){
            return "PBMsg: " + Integer.toString(pbmsg_len) + "," + Integer.toString(pbmsg_type) + "," + Integer.toString(pbmsg_from);
        } else {
            byte[] m = new byte[pbmsg_len-1];
            for (int i=0; i<pbmsg_len-1; i++) {
                m[i]=pbmsg[i];
            }
            return "PBMsg: " + Integer.toString(pbmsg_len) + "," + Integer.toString(pbmsg_type) + "," + Integer.toString(pbmsg_from) + ":" + new String(m);
        }
    }

    public int pbmsg_type;
    public int pbmsg_len;
    public int pbmsg_from; // filled in by BB server - using unqiue ID per client
    byte[] pbmsg;
}
