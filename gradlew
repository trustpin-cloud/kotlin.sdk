#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
  echo "$*"
}

die () {
  echo
  echo "$*"
  echo
  exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN* ) cygwin=true ;;
  MINGW* ) msys=true ;;
  Darwin* ) darwin=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
  JAVA_EXEC="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVA_EXEC" ] ; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  fi
else
  JAVA_EXEC="java"
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$msys" && ! "$nonstop"; then
  MAX_FD_LIMIT=$(ulimit -H -n)
  if [ $? -eq 0 ]; then
    if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ]; then
      MAX_FD=$MAX_FD_LIMIT
    fi
    ulimit -n "$MAX_FD" || warn "Could not set maximum file descriptor limit: $MAX_FD"
  else
    warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
  fi
fi

# For Cygwin or MSYS, convert paths to Windows format before running java
if "$cygwin" || "$msys"; then
  APP_HOME=$(cygpath --path --mixed "$APP_HOME")
  CLASSPATH=$(cygpath --path --mixed "$CLASSPATH")
fi

# Escape application args
save () {
  for i in "$@"; do
    echo "$i"
  done
}

APP_ARGS=$(save "$@")

# Collect all arguments for the java command, following the shell quoting and substitution rules
exec "$JAVA_EXEC" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"