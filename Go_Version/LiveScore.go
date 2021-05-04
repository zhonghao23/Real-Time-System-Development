package main

import (
	"bufio"
	"fmt"
	"log"
	"math/rand"
	"os"
	"strconv"
	"time"
)

type match struct {
	id int
	team1 string
	score1 int
	team2 string
	score2 int
}

var teams1 []string
var teams2 []string
var matches []match
var score1 int
var score2 int

func makeTimestamp() int64 { //get current time
	return time.Now().UnixNano() / int64(time.Millisecond)
}

func main() {
	startTime := makeTimestamp()
	openFile, error := os.Open("firstTeam.txt")
	if error != nil {
		log.Fatal(error)
	}
	openFile2, error2 := os.Open("secondTeam.txt")
	if error2 != nil {
		log.Fatal(error2)
	}

	scanFile := bufio.NewScanner(openFile)
	scanFile2 := bufio.NewScanner(openFile2)
	matchAmount := 0
	for scanFile.Scan() {
		teams1 = append(teams1, scanFile.Text())	//add first teams
		matchAmount++
	}
	if err := scanFile.Err(); err != nil {
		log.Fatal(err)
	}
	for scanFile2.Scan() {
		teams2 = append(teams2, scanFile2.Text())	//add second teams
	}
	if err := scanFile2.Err(); err != nil {
		log.Fatal(err)
	}
	openFile.Close()
	openFile2.Close()

	for i := 0; i < len(teams1); i++ {	//load all matches of the day
		m := match{id:i, team1:teams1[i], score1:0, team2:teams2[i], score2:0}
		matches = append(matches, m)
	}
	platform := make(chan match, 1000)
	for i := 0; i < 5; i++ {	//5 goroutines handling scores
		go scoreReceiver(platform, matchAmount)
	}
	go scoreDisplayer(platform, startTime)	//1 scheduled goroutine handling display
	for {}
}

func scoreReceiver(platform chan match, matchAmount int) {	//simulating situation of receiving live scores
	rand.Seed(int64(time.Now().Nanosecond()))
	workCount := 0
	for workCount < 10{
		matchId := rand.Intn(matchAmount)
		file, err := os.Open("randomScores.txt")
		if err != nil {
			log.Fatal(err)
		}
		scanner := bufio.NewScanner(file)
		random1 := rand.Intn(5)
		random2 := rand.Intn(5)
		count := 0
		for scanner.Scan() {
			if count == random1 {
				get, err := strconv.Atoi(scanner.Text())	//get random number from text file
				if err != nil{
					log.Fatal(err)
				}
				score1 = get
			}
			if count == random2 {
				get, err := strconv.Atoi(scanner.Text())	//get random number from text file
				if err != nil{
					log.Fatal(err)
				}
				score2 = get
			}
			count++
		}
		file.Close()
		m := match{id:matchId, team1:teams1[matchId], score1:score1, team2:teams2[matchId], score2:score2}
		//fmt.Println(m)
		platform <- m
		workCount++
		time.Sleep(time.Duration((30 + rand.Intn(10)) * 100) * time.Millisecond)	//random wait simulation
	}
}

func scoreDisplayer(platform chan match, startTime int64) {
	t := time.NewTicker(time.Second * 1)
displayLoop1: //the entire displayer loop
	for {
		for range t.C {
			select {
			case m := <-platform: //non-blocking reading/check if channel is empty
				matches[m.id].score1 = matches[m.id].score1 + m.score1 //update data
				matches[m.id].score2 = matches[m.id].score2 + m.score2
			displayLoop: //label for loop
				for {
					select {
					case m1 := <-platform: //get all results from channel
						matches[m1.id].score1 = matches[m1.id].score1 + m1.score1 //update data
						matches[m1.id].score2 = matches[m1.id].score2 + m1.score2
					default: //print updated results
						fmt.Println("\nLive Scores: (Periodic)")
						for i, allMatch := range matches {
							fmt.Printf("Match %v - %v \t %v\t : \t%v \t %v\n", i+1, allMatch.team1, allMatch.score1, allMatch.score2, allMatch.team2)
						}
						start := makeTimestamp()
						fmt.Println("-----Periodic Time:", start)
						break displayLoop //break loop after updating
					}
				}
			default: //aperiodic (when there is no result received in 1 sec, display result right away if got new result)
				fmt.Printf("\n\t---No new results in 1 second... Waiting...")
				select {
				case m := <-platform:
					matches[m.id].score1 = matches[m.id].score1 + m.score1 //update data
					matches[m.id].score2 = matches[m.id].score2 + m.score2
					fmt.Printf("\n\tLive Scores: (Aperiodic)")
					for i, allMatch := range matches {
						fmt.Printf("\n\tMatch %v - %v \t %v\t : \t%v \t %v", i+1, allMatch.team1, allMatch.score1, allMatch.score2, allMatch.team2)
					}
					start := makeTimestamp()
					fmt.Printf("\n\t-----Aperiodic Time: %v\n", start)
				case <-time.After(5 * time.Second):	//timeout if there's no new results in 5sec (simulation), matches for the day is ended
					break displayLoop1
				}
			}
		}
	}
	fmt.Println("\n-------------NO MORE RESULTS")
	fmt.Println("-------------ALL MATCHES ARE ENDED")
	endTime := makeTimestamp()
	elapsed := endTime - startTime
	fmt.Printf("Start Time: %v\nEnd Time: %v\nTime Elapsed: %v", startTime,endTime,elapsed)
	os.Exit(3)
}