#!/bin/sh

ProcessID=$(ps -ef | grep $1 | grep -v fail2ban-server | grep -v grep | grep -v killProcess | awk '{print $2}')

if [ -z "$ProcessID" ] ; then
	echo "false"
else
	kill $ProcessID
	echo "true"
fi


