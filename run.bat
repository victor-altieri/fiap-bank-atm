@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
    if exist "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" (
        call "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" package
    ) else (
        echo Maven nao encontrado. Configure Maven no PATH e Java 21 ou superior.
        pause
        exit /b 1
    )
) else (
    call mvn package
)
if errorlevel 1 (
    pause
    exit /b 1
)
java -cp "presentation\target\presentation-1.0-SNAPSHOT-runtime.jar;infrastructure\target\infrastructure-1.0-SNAPSHOT-runtime.jar" com.fiap.bank.atm.AtmApplication
if errorlevel 1 pause
endlocal
