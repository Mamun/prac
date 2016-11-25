#!/bin/bash

lein do clean, with-profiles uberjar uberjar
rm -rf dist
mkdir dist
mkdir dist/lib/
cp target/web-app.jar dist/
cp -r resources/* dist
#java -cp .:web-app.jar:lib/* app.server 9000

