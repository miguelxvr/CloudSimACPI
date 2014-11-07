#!/bin/bash

PATH=/usr/lib/lightdm/lightdm:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games

CSTATE="poweroff hibernate standby"
POWEROFF_OUT=poweroff.output_
RESULT=result.sleepstate.txt

rm -f *.pdf
rm -f results.*

#
# Gera grafico de linhas do simulador
#
echo "time;cstate;watts" > $RESULT
for ambiente in "poweroff"
do 
	echo "Gerando arquivo de saida para o ambiente $ambiente"
	
#	cat Sleep_example_simple.output_ | sed s/"Data center\'s energy is 0.00 W*sec"// > /tmp/Sleep.tmp
	cat Sleep_example_simple.output_ | sed s/.*[\ ]0[\.]00.*// > /tmp/Sleep.tmp
	grep "Data center's energy is" /tmp/Sleep.tmp | sed 's/^\(.*\):.*[i]s \(.*\) .*/\1;poweroff;\2/g' >> result.sleepstate.cloudsim.poweroff.txt
	rm -f tmp* 
done

#
# Gera grafico de linhas com resultados reais
#

for state in $CSTATE
do
	echo "time;host;watts" > result.sleepstate.real.$state.txt
	cat experimentos.$state.csv | while read line 
	do
		gline=`echo $line | grep Nodo`
		if [ $gline"x" != "x" ]
		then
			continue
		fi
		
		linen=$((linen + 1))
		echo "$linen;real-host1;$(echo $line | cut -d"," -f1)" >> result.sleepstate.real.$state.txt
		echo "$linen;real-host2;$(echo $line | cut -d"," -f2)" >> result.sleepstate.real.$state.txt
		echo "$linen;real-host3;$(echo $line | cut -d"," -f3)" >> result.sleepstate.real.$state.txt
		echo "$linen;real-host4;$(echo $line | cut -d"," -f4)" >> result.sleepstate.real.$state.txt
	done
done
