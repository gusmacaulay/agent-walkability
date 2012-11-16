Narrative:
In order to run agent based walkability models
As a java developer
I want to be able to request a set of agent paths, suitable for running as a simulation in openlayers

Scenario: Basic path generation
Given a path generator
Given a footpath network
Given a startpoint
Given an endpoint
When the path generator is asked for the shortest path/s
Then the correct shortest path/s will be provided
Then the path/s will have timestamps

Scenario: Walking speed parameters

Scenario: Traffic Lights and delays

Scenario: Time Limit

Scenario: Counting intersections crossed

Scenario: Validating inputs

Scenario: Multiple agents and destinations
Given a path generator
Given a footpath network
Given a startpoint
Given a set of endpoints
When the path generator is asked for the shortest path/s
Then the correct shortest path/s will be provided
Then the path/s will have timestamps