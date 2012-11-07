Narrative:
In order to run agent based walkability models
As a java developer
I want to be able to request a set of agent paths

Scenario: Basic path generation
Given a path generator
When the path generator is asked for a shortest path
Then the correct shortest path will be provided