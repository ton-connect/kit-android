@ECHO OFF
set APP_HOME=%~dp0
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m
set GRADLE_WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
java -jar "%GRADLE_WRAPPER_JAR%" %*
