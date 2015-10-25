#!/system/bin/sh
cd /data/data/com.sec.kbssm.happystream/files/var/logs/ &>> /data/data/com.sec.kbssm.happystream/files/var/logs/cleancache.out
echo "cd /data/data/com.sec.kbssm.happystream/files/var/logs/"
rm -r ./cache &>> /data/data/com.sec.kbssm.happystream/files/var/logs/cleancache.out
echo "rm -r ./cache"
rm ./*.* &>> /data/data/com.sec.kbssm.happystream/files/var/logs/cleancache.out
echo "rm ./*.*"
sh /data/data/com.sec.kbssm.happystream/files/squidz.sh &>> /data/data/com.sec.kbssm.happystream/files/var/logs/cleancache.out
echo "sh /data/data/com.sec.kbssm.happystream/files/squidz.sh"
