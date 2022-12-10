# HorizonSight
This application displays the distance and "shape" of the horizon. It uses Google Maps API to calculate and display 2 horizon shapes on a Google Map. The first, "ideal"
is a circle calculated purely using the users current altitude. The second "true" is calculated by taking into account the shape of the terrain around the user (between
the current location and the ideal altitude) and checking for points around the user that are higher than them. The settings tab allows the user to change the accuracy
of the true horizon calculation and set artificial locations to check the shape of a horizon in places other than their current location.

This is my first "true" Android project so some unexpected behaviour & unoptimal coding solutions are probably present. :) 
