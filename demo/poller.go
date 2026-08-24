package main

import "time"

// Demo data for Go Timer Leak Companion — used with `./gradlew runIde`
// to capture the real Marketplace screenshot. Open this file, the
// warning should appear on the `case <-time.After(...)` line.
func poll(ch chan int, done chan struct{}) {
	for {
		select {
		case v := <-ch:
			process(v)
		case <-time.After(5 * time.Second):
			// Leaks a new Timer on every loop iteration -- FLAGGED.
			return
		case <-done:
			return
		}
	}
}

func pollCorrect(ch chan int, done chan struct{}) {
	timer := time.NewTimer(5 * time.Second)
	defer timer.Stop()
	for {
		select {
		case v := <-ch:
			process(v)
		case <-timer.C:
			// Correctly reuses one Timer -- NOT flagged.
			return
		case <-done:
			return
		}
	}
}

func process(v int) {}
