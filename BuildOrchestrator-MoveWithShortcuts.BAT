
@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

ECHO SCRIPT DI LANCIO DEL PROGRAMMA BuildOrchestrator
ECHO PER FARGLI EFFETTUARE LA BUILD DI MoveWithShortcuts .
ECHO.

ECHO Cartella attuale :
CD
ECHO.

PAUSE

java -Dfile.encoding=UTF-8 ^
     -jar "C:\BuildOrchestrator\BuildOrchestrator.jar" ^
          "H:\Users\Davide\Davide\Sviluppo\REPOS\FileTools\MoveWithShortcuts\Build List MoveWithShortcuts ONLY.TXT"

PAUSE
