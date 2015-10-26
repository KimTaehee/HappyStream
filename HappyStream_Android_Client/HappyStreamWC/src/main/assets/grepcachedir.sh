#!/system/bin/sh
echo "cd /data/data/com.sec.kbssm.happystream/files/etc"
cd /data/data/com.sec.kbssm.happystream/files/etc &> /data/data/com.sec.kbssm.happystream/files/var/logs/grepcachedir.out
echo "cat squid.conf | busybox grep 'cache_dir ufs'"
cat squid.conf | busybox grep 'cache_dir ufs' &>> /data/data/com.sec.kbssm.happystream/files/var/logs/grepcachedir.out
echo `cat squid.conf | busybox grep 'cache_dir ufs'`
