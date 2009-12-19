#! /bin/sh

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

echo "Classpath is '${CLASSPATH}'."

exec "${java_cmd}" -version


