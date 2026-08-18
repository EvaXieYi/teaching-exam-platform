@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
if not "%MAVEN_WRAPPER_JAR%"=="" goto skip
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
:skip
if exist "%MAVEN_WRAPPER_JAR%" goto run
echo Missing %MAVEN_WRAPPER_JAR%
exit /b 1
:run
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
java -classpath "%MAVEN_WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
