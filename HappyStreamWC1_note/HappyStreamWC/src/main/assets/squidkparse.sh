#!/system/bin/sh
cd /data/data/com.sec.kbssm.happystream/files/sbin &> /data/data/com.sec.kbssm.happystream/files/var/logs/squidkparse.out
echo "cd /data/data/com.sec.kbssm.happystream/files/sbin"
./squid -k parse &>> /data/data/com.sec.kbssm.happystream/files/var/logs/squidkparse.out
echo "./squid -k parse"