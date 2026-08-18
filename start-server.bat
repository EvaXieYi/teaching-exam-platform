@echo off
setlocal
cd /d %~dp0exam-server
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
"..\\.tools\\apache-maven-3.9.9\\bin\\mvn.cmd" -s "..\\.tools\\settings.xml" spring-boot:run
