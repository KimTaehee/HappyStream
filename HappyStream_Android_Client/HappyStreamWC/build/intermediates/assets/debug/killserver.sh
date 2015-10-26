#!/system/bin/sh
$(cat /data/data/com.sec.kbssm.happystream/files/var/logs/squid.pid) &> /data/data/com.sec.kbssm.happystream/files/var/logs/killserver.out
kill -9 $(cat /data/data/com.sec.kbssm.happystream/files/var/logs/squid.pid)
su -c iptables -t nat -F &>> /data/data/com.sec.kbssm.happystream/files/var/logs/killserver.out