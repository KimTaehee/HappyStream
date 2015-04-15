#!/system/bin/sh
happystreamid=`busybox id -u`
echo "happystreamid: $happystreamid"

su -c iptables -t nat -F &> /data/data/com.sec.kbssm.happystream/files/var/logs/setip.out
echo "iptables -t nat -F"
su -c iptables -t nat -I PREROUTING -p 6 -i wlan0 --dport 80 -j REDIRECT --to-port 3128 &>> /data/data/com.sec.kbssm.happystream/files/var/logs/setip.out
echo "iptables -t nat -I PREROUTING -p 6 -i wlan0 --dport 80 -j REDIRECT --to-port 3128"

su -c iptables -t nat -I PREROUTING -p 6 -i rmnet_usb1 --dport 80 -j REDIRECT --to-port 3128 &>> /data/data/com.sec.kbssm.happystream/files/var/logs/setip.out
echo "iptables -t nat -I PREROUTING -p 6 -i rmnet_usb1 --dport 80 -j REDIRECT --to-port 3128"

su -c iptables -t nat -I OUTPUT -p 6 --dport 80 -j DNAT --to-destination 127.0.0.1:3128 &>> /data/data/com.sec.kbssm.happystream/files/var/logs/setip.out
echo "iptables -t nat -I OUTPUT -p 6 --dport 80 -j DNAT --to-destination 127.0.0.1:3128"

su -c iptables -t nat -I OUTPUT -p 6 --dport 80 -m owner --uid-owner $happystreamid -j ACCEPT &>> /data/data/com.sec.kbssm.happystream/files/var/logs/setip.out
echo "iptables -t nat -I OUTPUT -p 6 --dport 80 -m owner --uid-owner $happystreamid -j ACCEPT"

su -c "echo 1 > /proc/sys/net/ipv4/ip_forward"
echo "1 > /proc/sys/net/ipv4/ip_forward"