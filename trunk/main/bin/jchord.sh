#! /bin/sh

##
## Jchord executable script. Do not modify this file in any way.
## You can still affect the execution in several ways:
##
## Select JVM version you want by setting JAVA_HOME environment varibale.
##
## Any additional parameters can be passed to Java by setting environment
## variables JAVA_OPTS and/or JCHORD_OPTS. If boths of them are set and 
## contains conflicting options, the second one overwrites the first one.  
##
## Any command lines argumens passed to this scrip will be passed to 
## Jchord program without any change. 
##

##
## Detecting java command. If JAVA_HOME is set, we try to use it, 
## but verify wheter it is set correctly. If JAVA_HOME is not set, 
## we try to find 'java' command in PATH.
##
if [ ! -z "${JAVA_HOME}" ] ; then
	if [ -x "${JAVA_HOME}/bin/java" ] ; then
		java_cmd="${JAVA_HOME}/bin/java"
	else
		echo "JAVA_HOME is not set properly: '${JAVA_HOME}'"
		exit 1
	fi
else 
	if [ -x "`which java`" ] ; then
		java_cmd="java"
	else 
		echo "Cannot find java. Set JAVA_HOME properly."
		exit 1
	fi
fi

##
## Determine own installation directory, referenced as 'jchord_dir'.
##
current_working_directory="`pwd`"
script_dir=`dirname "${0}"`
cd "${script_dir}/.."
jchord_dir="`pwd`"
cd "${current_working_directory}"

##
## Constructing CLASSPATH. 
##
for library in ${jchord_dir}/lib/*.jar ; do 
	if [ -z "${CLASSPATH}" ] ; then
		CLASSPATH="${library}"
	else
		CLASSPATH="${CLASSPATH}:${library}"
	fi
done

export CLASSPATH

##
## SHOW MUST GO ON
##
exec "${java_cmd}" -Dchord.home.dir="${jchord_dir}" ${JAVA_OPTS} ${JCHORD_OPTS} 'chord.project.Project' "${@}"

