#!/bin/bash

python3 -m venv virtualtest
source virtualtest/bin/activate
echo "virtualtest activated"
pip3 install sqlalchemy bottle
python3 superservice.py migrate run
deactivate
echo "virtualtest deactivated"
