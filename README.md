# otel-lightstep-java

Fun little test to see how to use LightStep to instrument a set of java applications using Manual instrumentation. 

## Java notes
1. Spring app
2. API calling another API
3. Maven used to build package
4. OTel SDK included as well as LightStep Launcher


## Two Applications for a "GameStore" to buy consoles
1. server - This folder contains the component to check the availability of game consoles
2. client - This folder contains the API to call the server API to find out stock.  Simulates a mobile app. 

## Service Names
gamestore-server --> server

gamestore-mobileapp --> client

## Endpoints for each service for Postman calls to test

client --> http://127.0.0.1:8084/mobile/checkstock/{console}

server --> http://127.0.0.1:8083/availability/{console}

Values for {console} include "xbox", "ps3", "ps5"

Code inspired by this blog post and I give credit to those authors for the start to make it quick.
https://spring.io/blog/2021/02/09/metrics-and-tracing-better-together
