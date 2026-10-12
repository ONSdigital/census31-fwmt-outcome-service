> **THIS REPO IS SEEDED FROM 2021 CODE AND AS SUCH CURRENTLY NEEDS MODERNISATION!** (see also [SEEDING.md](SEEDING.md).)

# census31-fwmt-outcome-service
This service is a gateway between Total Mobile's COMET interface and FWMTG outcome service.

It receives a JSON response from TM, transforms it into a FWMT Canonical and places the message onto the Gateway.Outcome Pubsub topic

![](/outcomeservice-highlevel.png "outcomeservice highlevel diagram")

## Quick Start

To run:

    mvn spring-boot:run

## tm-outcome

![](tm-outcome.png "tm - census - outcome - mapping")

## Copyright
Copyright (C) 2018 Crown Copyright (Office for National Statistics)
