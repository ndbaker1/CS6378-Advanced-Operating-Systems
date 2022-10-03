#!/bin/bash

# Change this to your netid
netid=ndb180002

# Root directory of your project
PROJDIR=$(pwd)

# Directory where the config file is located on your local system
CONFIGLOCAL=$PROJDIR/config.txt

# Directory your java classes are in
BINDIR=$PROJDIR/build

# Your main project class
PROG=Node

n=0

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    i=$( echo $i | awk '{ print $1 }' )
    echo $i
    while [[ $n -lt $i ]]
    do
    	read line
    	p=$( echo $line | awk '{ print $1 }' )
      host=$( echo $line | awk '{ print $2 }' )


      #echo -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $CONFIGLOCAL $p &"
      ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $CONFIGLOCAL $p &

      n=$(( n + 1 ))
    done
)
