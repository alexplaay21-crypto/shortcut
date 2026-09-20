@rem Gradle Wrapper launcher (Windows).
@rem Finds Java, then starts org.gradle.wrapper.GradleWrapperMain from
@rem gradle\wrapper\gradle-wrapper.jar. `gradle wrapper --gradle-version 9.6.0`
@rem regenerates this file and gradlew with Gradle's own copies and creates the jar.

@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
for %%i in ("%DIRNAME%") do set APP_HOME=%%~fi

@rem Wrapper-process JVM options (not the build's).
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if exist "%CLASSPATH%" goto findJava
echo.
echo ERROR: %CLASSPATH% is missing.
echo.
echo The wrapper jar is a binary and is not part of this source drop. Create it once with
echo     gradle wrapper --gradle-version 9.6.0
echo or run the 'wrapper' task (Tasks ^> build setup) from Android Studio's Gradle tool window.
goto fail

:findJava
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2
goto fail

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal
