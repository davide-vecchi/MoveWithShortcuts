@ECHO OFF

REM Start the MoveWithShortcuts GUI using javaw, so that no console window
REM remains open. Any argument is forwarded to the program; the operation
REM options prefill the dialog fields.

SETLOCAL

SET JAR_PATH=%~dp0MoveWithShortcuts.jar

IF NOT EXIST "%JAR_PATH%" (
    ECHO.
    ECHO CRITICAL ERROR; the executable JAR was not found :
    ECHO   %JAR_PATH%
    ECHO.
    PAUSE
    EXIT /B 1
)

start "" javaw -jar "%JAR_PATH%" -gui %*

EXIT /B 0
