#!/system/bin/sh
cd /data/data/com.sec.kbssm.happystream/files/sbin &> /data/data/com.sec.kbssm.happystream/files/var/logs/squidrun.out
echo "cd /data/data/com.sec.kbssm.happystream/files/sbin"
./squid -DNCd1 &>> /data/data/com.sec.kbssm.happystream/files/var/logs/squidrun.out
echo "./squid -DNCd1"