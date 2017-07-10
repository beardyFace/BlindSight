package com.guidewatch;

import android.location.Location;

import java.util.Stack;

/**
 * Created by henry on 4/09/16.
 */
public class Path {

    private Stack<Location> locations = new Stack<Location>();
    private final Location START;
    private Location current_location;

    private final double THRESHOLD = 0.1;

    public Path(Location start){
        this.START = start;
        this.current_location = start;
        locations.push(start);
    }

    public boolean isNextLocation(){
        return locations.empty();
    }

    public Location getNextLocation(Location current){
        if(isNextLocation())
            return locations.pop();
        return null;
    }

    public void addLocation(Location current){
        //decide on how this will be stored to create the path
    }

    private void splitAndMerge(){

    }

//    function DouglasPeucker(PointList[], epsilon)
//    // Find the point with the maximum distance
//    dmax = 0
//    index = 0
//    end = length(PointList)
//    for i = 2 to ( end - 1) {
//        d = perpendicularDistance(PointList[i], Line(PointList[1], PointList[end]))
//        if ( d > dmax ) {
//            index = i
//            dmax = d
//        }
//    }
//    // If max distance is greater than epsilon, recursively simplify
//    if ( dmax > epsilon ) {
//        // Recursive call
//        recResults1[] = DouglasPeucker(PointList[1...index], epsilon)
//        recResults2[] = DouglasPeucker(PointList[index...end], epsilon)
//
//        // Build the result list
//        ResultList[] = {recResults1[1...length(recResults1)-1], recResults2[1...length(recResults2)]}
//    } else {
//        ResultList[] = {PointList[1], PointList[end]}
//    }
//    // Return the result
//    return ResultList[]
//            end
}
