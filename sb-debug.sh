#!/bin/bash

echo "usage: bash deploy-sb.sh [port: 9000 e.g.] [suspend:y or n]"

project_dir=`pwd`
echo $project_dir

port=8000
if [ ! -z $1 ];then
   port=$1
fi
echo "use ${port} as debug listen port."

is_suspend="n"
if [ ! -z $2 ];then
   if [ $2 == 'y' -o $2 == 'yes' ];then
      is_suspend="y"
   fi
fi
echo "suspend server when debugging? $is_suspend . "


mvn spring-boot:run -Drun.jvmArguments="-Ddev.project.dir=$project_dir -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=${is_suspend},address=${port}"
