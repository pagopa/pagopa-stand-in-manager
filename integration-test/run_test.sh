#!/bin/sh

if [ -z $ENV ]
then
  echo "DEV environment..."
else
  echo "Setting $ENV environment..."
    cp config/config.json config/config.json.orig
    dest="api.${ENV}."
    sed "s/api.dev./$dest/g" config/config.json > config/config.json.bkp
    sleep 1
    mv config/config.json.bkp config/config.json
    sleep 1
fi

if [ -z $TAGS ]
then
  TAGS="runnable"
fi

if [ -z $JUNIT ]
then
  junit=""
else
  junit="--junit-directory=junit --junit"
fi

echo "Run test ..."
rm -rf results junit

behave --format allure_behave.formatter:AllureFormatter -o results $junit --tags=$TAGS --summary --show-timings -v

rm -rf results/history && cp -R reports/history results/history 2>/dev/null

allure generate results -o reports --clean

if ! [ -z $ENV ]
then
  mv config/config.json.orig config/config.json
fi
