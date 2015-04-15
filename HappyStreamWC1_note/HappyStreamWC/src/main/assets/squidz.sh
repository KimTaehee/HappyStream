#!/system/bin/sh
cd /data/data/com.sec.kbssm.happystream/files/sbin &> /data/data/com.sec.kbssm.happystream/files/var/logs/squidz.out
echo "cd /data/data/com.sec.kbssm.happystream/files/sbin"
./squid -z &>> /data/data/com.sec.kbssm.happystream/files/var/logs/squidz.out
echo "./squid -z"