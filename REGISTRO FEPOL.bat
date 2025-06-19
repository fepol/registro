@echo off
REM Ir a la raíz del proyecto
cd /d "%~dp0"

REM Limpiar y recrear la carpeta bin
echo Limpiando carpeta bin...
rd /s /q JavaGUIApp\bin 2>nul
mkdir JavaGUIApp\bin

REM Buscar todos los .java y compilarlos
echo Compilando archivos Java...
for /r src\main %%f in (*.java) do (
    echo Compilando %%f
    javac -d JavaGUIApp\bin "%%f"
    if %errorlevel% neq 0 (
        echo Error de compilación en %%f
        pause
        exit /b
    )
)

REM Crear el archivo manifest
echo Main-Class: main.Main > manifest.txt
echo. >> manifest.txt

REM Crear el JAR
echo Empaquetando en programa-fepol.jar...
jar cfm JavaGUIApp\programa-fepol.jar manifest.txt -C JavaGUIApp\bin .

REM Ejecutar el JAR sin consola
echo Ejecutando aplicación...
start "" javaw -jar JavaGUIApp\programa-fepol.jar

REM Limpieza opcional
del manifest.txt

echo Proceso completado.
exit
