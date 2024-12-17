#!/bin/bash
javac src/main/java/pt/ulisboa/tecnico/motorist/$1/*.java
java -cp src/main/java/ pt.ulisboa.tecnico.motorist.$1.Main
