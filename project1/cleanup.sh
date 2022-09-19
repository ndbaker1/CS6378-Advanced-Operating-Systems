#!/bin/bash


# Change this to your netid
netid=ndb180002

# Root directory of your project
PROJDIR=$(pwd)

# Directory where the config file is located on your local system
CONFIGLOCAL=$PROJDIR/config.txt

n=0

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    i=$( echo $i | awk '{ print $1 }' )
    echo $i
    while [[ $n -lt $i ]]
    do
    	read line
        host=$( echo $line | awk '{ print $2 }' )

        echo $host
        ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host killall -u $netid &

        n=$(( n + 1 ))
    done
   
)


echo "Cleanup complete"
