#!/bin/bash

# Change this to your netid
#netid=ndb180002
netid=jrd180002

# Root directory of your project
PROJDIR=$(pwd)

# Directory where the config file is located on your local system
CONFIGLOCAL=$PROJDIR/config.txt

# Directory your java classes are in
BINDIR=$PROJDIR/build

# Your main project class
PROG=Node

n=0

conf_arg=$(java -cp $BINDIR Config $CONFIGLOCAL)
echo "$conf_arg"

if [ -n "$1" ]; then
  echo "java -cp $BINDIR $PROG $1 $PROJDIR "$conf_arg" &"
  java -cp $BINDIR $PROG $1 $PROJDIR "$conf_arg"
  exit
fi

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

      echo -e "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $p $PROJDIR "$conf_arg" &"
      ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $p $PROJDIR "$conf_arg" &

      n=$(( n + 1 ))
    done
)
