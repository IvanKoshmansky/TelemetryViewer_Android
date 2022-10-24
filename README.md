# TelemetryViewer

## What is this project about? ##

This project provides the ability to view data from telemetry devices, which measures parameters of the corrosion protection. Devices transmits these parameters to the data collection server, and then this mobile app reads data from the MS SQL database that the server is working with.

## Project features ##

Unlike the most common scheme, when the client works with the server through the HTTP protocol, in this project a different scheme is implemented according to the requirements of the customer. 
This project uses a direct connection to the database MS SQL via Java JTDS library.
This library provides access to the MS SQL database (and to others too) through standard SQL language constructs, as well as the ability to execute stored procedures.
The functionality for this solution is located in the `network` package.
