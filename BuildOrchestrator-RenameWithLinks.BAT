
@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

ECHO SCRIPT DI LANCIO DEL PROGRAMMA BuildOrchestrator
ECHO PER FARGLI EFFETTUARE LA BUILD DI RenameWithLinks
ECHO COMPRESE LE JavaLibraries USATE DA RenameWithLinks .
ECHO.

ECHO Cartella attuale :
CD
ECHO.

PAUSE

java -jar C:\BuildOrchestrator\BuildOrchestrator.jar ^
          "H:\Users\Davide\Davide\Sviluppo\REPOS\FileTools\RenameWithLinks\Build List RenameWithLinks.TXT"

PAUSE
