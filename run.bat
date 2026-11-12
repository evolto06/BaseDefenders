@echo off
setlocal
cd /d "%~dp0"
javac -d bin -sourcepath src src\main\Main.java || exit /b 1
java -cp bin main.Main
