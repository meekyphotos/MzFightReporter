@echo off
where java >nul 2>nul
if %errorlevel%==1 (
    @echo PROBLEM DETECTED: Java was not detected.  Install at www.java.com.
    @echo.
    pause
    exit
)

java -jar MzApp-Latest.jar CheckUpdater

IF EXIST zcore.jar (
    move /y zcore.jar MzApp-Latest.jar
)

java -jar MzApp-Latest.jar FileWatcher "%cd%"
pause