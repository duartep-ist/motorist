@REM Usage: .\run.bat program "arg1 arg2 ..."
@mvn -q exec:java "-Dexec.mainClass=pt.ulisboa.tecnico.motorist.%1.Main" -Dexec.args=%2
