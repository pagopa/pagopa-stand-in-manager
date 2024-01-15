#!/usr/bin/env bash

folder="certificate"
if [ ! -d "$folder" ]; then
  mkdir $folder
fi

cd $folder
certbot certonly --standalone --agree-tos --preferred-challenges http -d mockec.ddns.net --config-dir . --logs-dir . --work-dir .