#!/bin/bash

# DO NOT CHANGE -------------------
KONG_SERVER_ADDRESS=$(cat kong_addr.tmp)
USER_URL="http://$(cat ../Microservices/user_addr.tmp):8080/"
CAR_URL="http://$(cat ../Microservices/car_addr.tmp):8080/"
APILOT_URL="http://$(cat ../Microservices/apilot_addr.tmp):8080/"

# create services
curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/" \
--data "name=user-microservice" \
--data "url=${USER_URL}"\

curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/" \
--data "name=car-microservice" \
--data "url=${CAR_URL}"\

curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/" \
--data "name=apilot-microservice" \
--data "url=${APILOT_URL}"\

# create routes
curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/user-microservice/routes" \
--data "hosts[]=user.com"

curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/car-microservice/routes" \
--data "hosts[]=car.com"

curl -i -X POST \
--url "http://${KONG_SERVER_ADDRESS}:8001/services/apilot-microservice/routes" \
--data "hosts[]=apilot.com"