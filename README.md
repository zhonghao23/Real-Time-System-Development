# Real-Time-System-Development
Simulates a Soft Real Time System (Basketball Live Scorebord System) with Threads, Synchronous and Asynchronous Communication in Java and Go

# Motivation
This project simulates the live scoreboard system by implementing the real time concepts. The Java and Go version are developed
to compare the real time performance between these two languages.

# Tech/Framework Used
1. Java
2. Golang

# General Concepts of the Real Time System
There are threads which are generating the real time scores in a random amount of time and also thread which is retrieving the scores generated and display it.

The system would perform synchronous communication (retrieving real time results every 1 second and display) between threads and 

the asynchronous communication (continuously wait for any new results and display it immediately if there is no new results in 1 second)

** The asynchronous communication is required because if there is a new result in 1.1 second but the system will only retrieve it every 1 second which makes the new result will only be displayed after another 0.9 second

** It might seems fine with late result in this soft real time system but in a hard real time system, delay in any second might cause a severe issues

# Files
1. firstTeam.txt - Stores the team name of first group.
2. secondTeam.txt - Stores the team name of second group.
3. randomScores.txt - Various form of scores that will be taken randomly to simulate a random scores retrieved from other source. 

# Performance Evaluation
1. The average memory usage for the same system executed in Java will be 942637.67 bytes while it is 225617.73 in Golang. (Golang is better!)
2. The average time taken for garbage collection in Java takes 6239063 nanoseconds while it takes 998418.33 nanoseconds in Golang. (Java is better!)
3. The average runtime for the same amount of execution/simulation in Java takes 38413.99 milliseconds while it takes 38003.33 milliseconds in Golang. (Golang is better!)

# Verdict
Golang is proved to be a better language based on the benchmark test.

