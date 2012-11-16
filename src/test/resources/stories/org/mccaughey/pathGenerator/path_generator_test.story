Narrative:
In order to run agent based walkability models
As a java developer
I want to be able to request a set of agent paths, suitable for running as a simulation in openlayers

Scenario: Basic path generation
Given a path generator
Given a footpath network
Given a startpoint
Given an endpoint
When the path generator is asked for a shortest path
Then the correct shortest path will be provided
Then the path will have timestamps

Scenario: Traffic Lights and delays

Scenario: Time Limit

Scenario: Counting intersections crossed

Scenario: Validating inputs