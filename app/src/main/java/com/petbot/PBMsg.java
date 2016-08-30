package com.petbot;

/**
 * Created by miskodzamba on 16-08-30.
 */
public class PBMsg {
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
pbmsg * recv_all_pbmsg(pbsock * pbs,int read_all);
size_t send_pbmsg(pbsock *, pbmsg *m);
//Send / recv pbmsg over file descriptor
pbmsg * recv_fd_pbmsg(int fd);
pbmsg * recv_all_fd_pbmsg(int fd,int read_all);
size_t send_fd_pbmsg(int fd, pbmsg *m);
*/
    public int pbmsg_type;
    public int pbmsg_len;
    public int pbmsg_from; // filled in by BB server - using unqiue ID per client
    byte[] pbmsg;
}
