#!/system/bin/sh
ps squid | busybox grep squid &> /data/data/com.sec.kbssm.happystream/files/var/logs/pssquid.out
echo "ps squid | busybox grep squid"
