@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Compilation de Sworld Waystones pour Minecraft 26.3...
call gradlew.bat build
if errorlevel 1 (
 echo Echec. Verifie que Java 25 est installe. Copie le message d'erreur pour obtenir de l'aide.
 pause
 exit /b 1
)
echo Termine ! Le mod se trouve dans build\libs.
explorer build\libs
pause
