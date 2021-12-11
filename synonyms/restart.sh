#!/usr/bin/env bash

kill `lsof -t -i:12345`
python3 server.py