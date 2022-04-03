#!/bin/sh
ST_DIR=studentdirs

for f in `ls $ST_DIR` 
do
	./test_linux.sh $ST_DIR/$f
done

