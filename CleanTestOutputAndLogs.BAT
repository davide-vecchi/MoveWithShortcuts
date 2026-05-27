@ECHO OFF
REM 16:25 2026-05-13

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

@ECHO ON

CD /D "H:\Users\Davide\Davide\Sviluppo\REPOS\FileTools\RenameWithLinks\"

DEL /S /Q "*.LOG"

DEL /S /Q "*.TMP"


RMDIR /S /Q "test-output\"

PAUSE


CD /D "C:\RenameWithLinks"

DEL /S /Q "*.LOG"

DEL /S /Q "*.TMP"

PAUSE
