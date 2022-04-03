pimport java.time.format.TextStyle;
import java.util.ArrayList;
import java.lang.*;
import java.util.Locale;

public class Location {
    private String location_name;
    private double latitude;
    private double longitude;
    private ArrayList<Flight> arriving_f;
    private ArrayList<Flight> departing_f;
    private double demand_coe;

    // Create a constructor
    public Location(String name, double latitude, double longitude, double demand) {
        this.location_name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.arriving_f = new ArrayList<>();
        this.departing_f = new ArrayList<>();
        this.demand_coe = demand;
    }

    // To check if location's latitide is within the valid range [-85,85]
    public static boolean is_Valid_lat(String latitude) {
        boolean is_valid = true;
        try {
            double num = Double.parseDouble(latitude);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Double.parseDouble(latitude) < -85 || Double.parseDouble(latitude) > 85) {
            is_valid = false;
        }
        return is_valid;
    }

    // To check if location's longitude is within the valid range [-180, 180]
    public static boolean is_Valid_lon(String longitude) {
        boolean is_valid = true;
        try {
            double num = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Double.parseDouble(longitude) < -180 || Double.parseDouble(longitude) > 180) {
            is_valid = false;
        }
        return is_valid;
    }

    // To check if the demand coe is within the valid range [-1, 1]
    public static boolean is_Valid_coe(String demand_coe) {
        boolean is_valid = true;
        try {
            double num = Double.parseDouble(demand_coe);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Double.parseDouble(demand_coe) < -1 || Double.parseDouble(demand_coe) > 1) {
            is_valid = false;
        }
        return is_valid;
    }

    //Implement the Haversine formula - return value in kilometres
    public static double distance(Location l1, Location l2) {
        // The method is referenced from the website "IGISMAP"
        // https://www.igismap.com/haversine-formula-calculate-geographic-distance-earth/
        double Lat1 = l1.latitude;
        double Lat2 = l2.latitude;
        double Lon1 = l1.longitude;
        double Lon2 = l2.longitude;
        // Earth's radius in km
        int earth_radius = 6371;
        // Noticed that angles have to be in radians but given in degrees! Need to convert into radians!
        // Difference of latitude
        double diff_lat = Math.toRadians(Lat1 - Lat2);
        // Difference of longitude
        double diff_lon = Math.toRadians(Lon1 - Lon2);
        // Apply Haversine formula:
        double a = Math.pow(Math.sin(diff_lat/2), 2) + Math.cos(Math.toRadians(Lat1)) * Math.cos(Math.toRadians(Lat2)) *
                Math.pow(Math.sin(diff_lon/2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earth_radius * c;
        return distance;
    }

    public void addArrival(Flight f) {
        arriving_f.add(f);
    }

    public void addDeparture(Flight f) {
        departing_f.add(f);
    }

    /**
     * Check to see if Flight f can depart from this location.
     * If there is a clash, the clashing flight string is returned, otherwise null is returned.
     * A conflict is determined by if any other flights are arriving or departing at this location within an hour of this flight's departure time.
     * @param f The flight to check.
     * @return "Flight <id> [departing/arriving] from <name> on <clashingFlightTime>". Return null if there is no clash.
     */
    public String hasRunwayDepartureSpace(Flight f) {
        // To check if there are multiple conflicts
        ArrayList <Flight> dep_conflicts = new ArrayList<>();
        ArrayList <Flight> arr_conflicts = new ArrayList<>();
        //Check departures first
        if(departing_f != null) {
            for (Flight flight : departing_f) {
                // If there is a clashing flight departing within an hour of this flight’s departure time
                if (Flight.time_between(f.getDep_local_time(), flight.getDep_local_time(),
                        f.getDep_dayofWeek(), flight.getDep_dayofWeek()) < 60) {
                    dep_conflicts.add(flight);
                }
            }
        }
        //Then check arrivals
        if(arriving_f != null) {
            for (Flight flight : arriving_f) {
                // If there is a clashing flight arriving within an hour of this flight’s departure time
                if (Flight.time_between(f.getDep_local_time(), Flight.arrival_l_time(flight),
                        f.getDep_dayofWeek(), Flight.arrival_day_week(flight)) < 60) {
                    arr_conflicts.add(flight);
                }
            }
        }
        // Now, return the warning message.
        if(conflictKing(dep_conflicts, arr_conflicts) != null) {
            return conflictKing(dep_conflicts, arr_conflicts);
        }
        // If this location's schedule is empty
        if(arriving_f == null && departing_f == null) {
            addDeparture(f);
            return null;
        }
        // If no conflicts
        addDeparture(f);
        return null;
    }

    /**
     * Check to see if Flight f can arrive at this location.
     * A conflict is determined by if any other flights are arriving or departing at this location within an hour of this flight's arrival time.
     * @param f The flight to check.
     * @return String representing the clashing flight, or null if there is no clash. Eg. "Flight <id> [departing/arriving] from <name> on <clashingFlightTime>"
     */
    public String hasRunwayArrivalSpace(Flight f) {
        // To check if there are multiple conflicts
        ArrayList <Flight> dep_conflicts1 = new ArrayList<>();
        ArrayList <Flight> arr_conflicts1 = new ArrayList<>();
        //Check departures first
        if(departing_f != null) {
            for (Flight flight : departing_f) {
                if (Flight.time_between(Flight.arrival_l_time(f), flight.getDep_local_time(),
                        Flight.arrival_day_week(f), flight.getDep_dayofWeek()) < 60) {
                    dep_conflicts1.add(flight);
                }
            }
        }
        //Then check arrivals
        if(arriving_f != null) {
            for (Flight flight : arriving_f) {
                // If there is a clashing flight arriving within an hour of this flight’s arriving time
                if (Flight.time_between(Flight.arrival_l_time(f), Flight.arrival_l_time(flight),
                        Flight.arrival_day_week(f), Flight.arrival_day_week(flight)) < 60){
                    arr_conflicts1.add(flight);
                }
            }
        }
        // Return the warning message.
        if(conflictKing(dep_conflicts1, arr_conflicts1) != null) {
            return conflictKing(dep_conflicts1, arr_conflicts1);
        }
        // If this location's schedule is empty
        if(arriving_f == null && departing_f == null) {
            addArrival(f);
            return null;
        }
        // If no conflicts
        addArrival(f);
        return null;
    }

    public static String conflictKing(ArrayList<Flight> dep_conflicts, ArrayList<Flight> arr_conflicts) {
        // First check if the array lists is empty
        boolean dep_empty = dep_conflicts.isEmpty();
        boolean arr_empty = arr_conflicts.isEmpty();
        Flight conflict_winner_dep;
        Flight conflict_winner_arr;
        // if there are multiple conflicts for both departure and arrival... Noooooo T_T
        if (!dep_empty && !arr_empty) {
            conflict_winner_dep = dep_conflicts.get(0);
            conflict_winner_arr = arr_conflicts.get(0);
            int winner = conflict_winner_dep.getDep_local_time().compareTo(Flight.arrival_l_time(conflict_winner_arr));
            if(winner > 0) {
                return "Scheduling conflict! This flight clashes with Flight" + " " + conflict_winner_dep.getFlight_id()
                        + " " + "departing from" + " " + conflict_winner_dep.getDep_location().location_name + " " + "on" + " "
                        + conflict_winner_dep.getDep_dayofWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                        conflict_winner_dep.getDep_local_time().toString() + ".";
            } else if (winner < 0) {
                return "Scheduling conflict! This flight clashes with Flight" + " " + conflict_winner_arr.getFlight_id()
                        + " " + "arriving at" + " " + conflict_winner_arr.getArr_location().location_name + " " + "on" + " "
                        + Flight.arrival_day_week(conflict_winner_arr).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                        Flight.arrival_l_time(conflict_winner_arr).toString() + ".";
            }
        }
        // If there are multiple conflicts only for departure flight
        if(!dep_empty && arr_empty) {
            conflict_winner_dep = dep_conflicts.get(0);
            // Find the latest one in departure flight list
            if(dep_conflicts.size() > 1) {
                for (int i = 1; i < dep_conflicts.size(); i++) {
                    int value = conflict_winner_dep.getDep_local_time().compareTo(dep_conflicts.get(i).getDep_local_time());
                    if(value < 0)  {
                        conflict_winner_dep = dep_conflicts.get(i);
                    }
                }
            }
            return "Scheduling conflict! This flight clashes with Flight" + " " + conflict_winner_dep.getFlight_id()
                    + " " + "departing from " + conflict_winner_dep.getDep_location().location_name + " " + "on" + " "
                    + conflict_winner_dep.getDep_dayofWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                    conflict_winner_dep.getDep_local_time().toString() + ".";
        }

        // If there are multiple conflicts only for arrival flight
        if(dep_empty && !arr_empty) {
            conflict_winner_arr = arr_conflicts.get(0);
            if(arr_conflicts.size() > 1) {
                for (int j = 1; j < arr_conflicts.size(); j++) {
                    int valuee = Flight.arrival_l_time(conflict_winner_arr).compareTo(Flight.arrival_l_time(arr_conflicts.get(j)));
                    if(valuee < 0)  {
                        conflict_winner_arr = arr_conflicts.get(j);
                    }
                }
            }
            return "Scheduling conflict! This flight clashes with Flight" + " " + conflict_winner_arr.getFlight_id()
                    + " " + "arriving at " + conflict_winner_arr.getArr_location().location_name + " " + "on" + " "
                    + Flight.arrival_day_week(conflict_winner_arr).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                    Flight.arrival_l_time(conflict_winner_arr).toString() + ".";
        }
        // If there's no conflicts (shame...)
        return null;
    }

    // toString method for export
    public String location_toString() {
        return location_name + "," + latitude + "," + longitude + "," + demand_coe + "\n";
    }
    // The following are the setters & getters for private attributes :)

    // Get location's latitude
    public double getLatitude() {

        return latitude;
    }

    // Get location's longitude
    public double getLongitude() {

        return longitude;
    }

    // Get location name
    public String getLocation_name() {

        return location_name;
    }

    // Get demand coe
    public double getDemand_coe() {

        return demand_coe;
    }

    // Get departing flight list
    public ArrayList<Flight> getDeparting_f() {

        return departing_f;
    }

    // Get arriving flight list
    public ArrayList<Flight> getArriving_f() {

        return arriving_f;
    }
}