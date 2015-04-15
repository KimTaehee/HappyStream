#!/system/bin/sh
dir_mem=$1
busybox sed -r -i 's/cache [0-9]+/cache '$1'/' /data/data/com.sec.kbssm.happystream/files/etc/squid.conf
echo "sed -r -i 's/cache [0-9]+/cache '$1'/' /data/data/com.sec.kbssm.happystream/files/etc/squid.conf"
cd /data/data/com.sec.kbssm.happystream/files/sbin &> /data/data/com.sec.kbssm.happystream/files/var/logs/squidreconfigure.out
./squid -k reconfigure &>> /data/data/com.sec.kbssm.happystream/files/var/logs/squidreconfigure.out
echo "./squid -k reconfigure"