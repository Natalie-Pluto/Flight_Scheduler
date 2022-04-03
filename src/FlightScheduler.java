import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.*;
import java.io.*;
import java.time.*;
import java.io.File;
import java.io.IOException;

public class FlightScheduler {
    private ArrayList<Flight> list_of_flights;
    private ArrayList<Location> list_of_locations;
    private int id_counter = 0;

    private static FlightScheduler instance;

    public static void main(String[] args) {
        instance = new FlightScheduler(args);
        instance.run();
    }

    public static FlightScheduler getInstance() {

        return instance;
    }

    public FlightScheduler(String[] args) {
        this.list_of_flights = new ArrayList<>();
        this.list_of_locations = new ArrayList<>();
    }

    public void run() {
        // Do not use System.exit() anywhere in your code,
        // otherwise it will also exit the auto test suite.
        // Also, do not use static attributes otherwise
        // they will maintain the same values between testcases.

        // Keep getting input until end is equal to 1.
        int end = 0;
        Scanner user_input = new Scanner(System.in);
        System.out.print("User: ");
        while (end != 1) {
            // Store inputs as strings into an array
            // Separate the inputs by spaces :)
            // Get user's input...
            String[] input = user_input.nextLine().split(" ");
            // If nothing is entered at all, print the error message
            if (input.length <= 0) {
                System.out.println("Invalid command. Type 'help' for a list of commands.");
            }
            // Use switch and case to distinguish the commands
            // First determine if it's FLIGHT LOCATION TRAVEL etc commands
            // Separate out the first index...
            if (input.length > 0) {
                switch (input[0].toUpperCase()) {
                    // FLIGHTS command: list all available flights ordered by departure time, then departure location name
                    case ("FLIGHTS"):
                        // Sort the flights ordered by departure time, then departure location name
                        ArrayList<Flight> sorted_list = sorted(list_of_flights);
                        // Print the flight list
                        System.out.println("Flights");
                        System.out.println("-------------------------------------------------------");
                        System.out.println("ID   Departure   Arrival     Source --> Destination");
                        System.out.println("-------------------------------------------------------");
                        // No flights are available
                        if (list_of_flights.isEmpty()) {
                            System.out.println("(None)");
                        }
                        for (Flight flight : sorted_list) {
                            String dep_day = flight.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            // Departure time
                            String dep_time = flight.getDep_local_time().toString();
                            // Arrival day
                            String arr_day = Flight.arrival_day_week(flight).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            // Arrival time
                            String arr_time = Flight.arrival_l_time(flight).toString();
                            System.out.printf("%4d %s %s   %s %s   %s --> %s\n", flight.getFlight_id(),
                                    dep_day, dep_time, arr_day, arr_time, flight.getDep_location().getLocation_name(),
                                    flight.getArr_location().getLocation_name());
                        }
                        break;
                    case ("FLIGHT"):
                        // Error checking: no parameters given
                        if (input.length == 1) {
                            System.out.println("Usage:\nFLIGHT <id> [BOOK/REMOVE/RESET] [num]\n" +
                                    "FLIGHT ADD <departure time> <from> <to> <capacity>\nFLIGHT IMPORT/EXPORT <filename>");
                            break;
                        }
                        // For "ADD', "EXPORT", "IMPORT"
                        switch (input[1].toUpperCase()) {
                            case ("ADD"):
                                // Check the possible error first
                                if (input.length >= 7) {
                                    // 1) Time added was not in the correct format, for example “Monday 18:00”
                                    String day = input[2];
                                    if (!is_valid_day(day)) {
                                        System.out.println("Invalid departure time. Use the format <day_of_week> " +
                                                "<hour:minute>, with 24h time.");
                                        break;
                                    }

                                    String time = input[3];
                                    if (!is_valid_time(time)) {
                                        System.out.println("Invalid departure time. Use the format <day_of_week> " +
                                                "<hour:minute>, with 24h time.");
                                        break;
                                    }
                                    String dep_location = input[4];
                                    String arr_location = input[5];
                                    // 2) Starting location was not in the database but ending location was in the database.
                                    if (is_location_exist(dep_location, list_of_locations) == null) {
                                        System.out.println("Invalid starting location.");
                                        break;
                                    }
                                    // 3) Ending location was not in the database but starting location was in the database.
                                    if (is_location_exist(arr_location, list_of_locations) == null) {
                                        System.out.println("Invalid ending location.");
                                        break;
                                    }
                                /* 4) Both Starting & Ending location were not in the database
                                    ed testcase doesn't like it but I like it so i gonna keep it here >:^/
                                if (is_location_exist(arr_location, list_of_locations) == null &&
                                        is_location_exist(dep_location, list_of_locations) == null) {
                                    System.out.println("Invalid starting location.");
                                    System.out.println("Invalid ending location.");
                                    break;
                                }*/
                                    // 5) Capacity was not a positive integer.
                                    int cap = Integer.parseInt(input[6]);
                                    if (cap <= 0) {
                                        System.out.println("Invalid positive integer capacity.");
                                        break;
                                    }

                                    // 6) The two locations entered were the same
                                    String l1 = input[4];
                                    String l2 = input[5];
                                    if (l1.equalsIgnoreCase(l2)) {
                                        System.out.println("Source and destination cannot be the same place.");
                                        break;
                                    }
                                    Location dep_l = is_location_exist(dep_location, list_of_locations);
                                    Location arr_l = is_location_exist(arr_location, list_of_locations);
                                    Flight newFlight = new Flight(id_counter, input[2] + " " + input[3], dep_l,
                                            arr_l, Integer.parseInt(input[6]));
                                    if (dep_l != null && arr_l != null) {
                                        // 8) No runways are available for this flight at the designated location at that time.
                                        if (dep_l.hasRunwayDepartureSpace(newFlight) != null) {
                                            String warning = dep_l.hasRunwayDepartureSpace(newFlight);
                                            System.out.println(warning);
                                            break;
                                        }
                                        // 9) No runways are available for this flight at the designated location at that time.
                                        if (arr_l.hasRunwayArrivalSpace(newFlight) != null) {
                                            String warning = arr_l.hasRunwayArrivalSpace(newFlight);
                                            System.out.println(warning);
                                            break;
                                        }
                                    }
                                    // Now we can add the flight to the list
                                    list_of_flights.add(newFlight);
                                    System.out.printf("Successfully added Flight %d.\n", newFlight.getFlight_id());
                                    id_counter = id_counter + 1;
                                }
                                // 7) Not enough command arguments given.
                                if (input.length < 7) {
                                    System.out.println("Usage:   FLIGHT ADD <departure time> <from> <to> <capacity>" +
                                            "\nExample: FLIGHT ADD Monday 18:00 Sydney Melbourne 120");
                                    break;
                                }
                                break;
                            case ("IMPORT"):
                                importFlights(input);
                                break;
                            case ("EXPORT"):
                                exportFlights(input);
                                break;
                            // <id> : view information about a flight
                            default:
                                if (input.length == 2) {
                                    // Error checking
                                    try {
                                        Integer.parseInt(input[1]);
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (!is_id_exist(Integer.parseInt(input[1]), list_of_flights)) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (Integer.parseInt(input[1]) < 0) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    // Print the information
                                    for (Flight f : list_of_flights) {
                                        if (f.getFlight_id() == Integer.parseInt(input[1])) {
                                            // Departure day
                                            String dep_day = f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                                            // Departure time
                                            String dep_time = f.getDep_local_time().toString();
                                            // Arrival day
                                            String arr_day = Flight.arrival_day_week(f).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                                            // Get duration time
                                            int duration_time = f.getDuration();
                                            int hours = duration_time / 60;
                                            int minuets = duration_time % 60;
                                            // The method of formatting number is taken from Java67
                                            // https://www.java67.com/2015/06/how-to-format-numbers-in-java.html#:~:text=
                                            // In%20order%20to%20print%20numbers,number%20starting%20from%20the%20right.
                                            DecimalFormat format = new DecimalFormat("#.##");
                                            format.setGroupingUsed(true);
                                            format.setGroupingSize(3);
                                            // Arrival time
                                            String arr_time = Flight.arrival_l_time(f).toString();
                                            System.out.printf("Flight %d\n", Integer.parseInt(input[1]));
                                            System.out.printf("Departure:    %s %s %s\n", dep_day, dep_time,
                                                    f.getDep_location().getLocation_name());
                                            System.out.printf("Arrival:      %s %s %s\n", arr_day, arr_time,
                                                    f.getArr_location().getLocation_name());
                                            System.out.printf("Distance:     %skm\n", format.format((int) Math.round(f.getDistance())));
                                            System.out.printf("Duration:     %dh %dm\n", hours, minuets);
                                            System.out.printf("Ticket Cost:  $%.2f\n", f.getTicketPrice());
                                            System.out.printf("Passengers:   %d/%d\n", f.getNum_of_booked(), f.getCapacity());
                                            break;
                                        }
                                    }
                                }
                                break;
                        }
                        // For the rest of commands followed by ID
                        if (input.length > 2) {
                            switch (input[2].toUpperCase()) {
                                case ("BOOK"):
                                    // Error checking
                                    try {
                                        Integer.parseInt(input[1]);
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (!is_id_exist(Integer.parseInt(input[1]), list_of_flights)) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (Integer.parseInt(input[1]) < 0) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }

                                    // The number of passengers entered was not a valid positive integer.
                                    if (input.length > 3) {
                                        try {
                                            Integer.parseInt(input[3]);
                                        } catch (NumberFormatException e) {
                                            System.out.println("Invalid number of passengers to book.");
                                            break;
                                        }
                                        if (Integer.parseInt(input[3]) <= 0) {
                                            System.out.println("Invalid number of passengers to book.");
                                            break;
                                        }
                                    }
                                    // Given more parameters than needed
                                    // I guess will have no affection?
                                    // Let's start booking then!
                                    // If no number is given, only book one passenger
                                    int bookings = 1;
                                    if (input.length > 3) {
                                        bookings = Integer.parseInt(input[3]);
                                    }
                                    double total_price;
                                    for (Flight f : list_of_flights) {
                                        if (f.getFlight_id() == Integer.parseInt(input[1])) {
                                            // Get the remaining seats
                                            int seats_left = f.getCapacity() - f.getNum_of_booked();
                                            // If the seats left is less or equal to the booking requirement
                                            if (bookings >= seats_left && seats_left != 0) {
                                                if (bookings != seats_left) {
                                                    bookings = seats_left;
                                                }
                                                total_price = f.book(bookings);
                                                System.out.printf("Booked %d passengers on flight %d for a total cost of $%.2f\n",
                                                        bookings, Integer.parseInt(input[1]), total_price);
                                                System.out.println("Flight is now full.");
                                            } else if (seats_left > bookings) { // If the seats left is more than the booking requirement
                                                total_price = f.book(bookings);
                                                System.out.printf("Booked %d passengers on flight %d for a total cost of $%.2f\n",
                                                        bookings, Integer.parseInt(input[1]), total_price);
                                            }
                                            // If there's no remaining seat
                                            if (seats_left == 0) {
                                                System.out.printf("Booked 0 passengers on flight %d for a total cost of $0.00\n",
                                                        Integer.parseInt(input[1]));
                                                System.out.println("Flight is now full.");
                                            }
                                            break;
                                        }
                                    }
                                    break;
                                case ("REMOVE"):
                                    // Error checking
                                    try {
                                        Integer.parseInt(input[1]);
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (!is_id_exist(Integer.parseInt(input[1]), list_of_flights)) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }

                                    if (Integer.parseInt(input[1]) < 0) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    // Find the target flight, remove it and print the message
                                    for (Flight f : list_of_flights) {
                                        if (f.getFlight_id() == Integer.parseInt(input[1])) {
                                            // Departure day
                                            String dep_day = f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                                            // Departure time
                                            String dep_time = f.getDep_local_time().toString();
                                            // Print the flight information
                                            System.out.printf("Removed Flight %d, %s %s %s --> %s, from the flight schedule.\n",
                                                    Integer.parseInt(input[1]), dep_day, dep_time, f.getDep_location().getLocation_name(),
                                                    f.getArr_location().getLocation_name());
                                            list_of_flights.remove(f);
                                            f.getDep_location().getDeparting_f().remove(f);
                                            f.getArr_location().getArriving_f().remove(f);
                                            break;
                                        }
                                    }
                                    break;

                                case ("RESET"):
                                    // Error checking
                                    try {
                                        Integer.parseInt(input[1]);
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (!is_id_exist(Integer.parseInt(input[1]), list_of_flights)) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }
                                    if (Integer.parseInt(input[1]) < 0) {
                                        System.out.println("Invalid Flight ID.");
                                        break;
                                    }

                                    // Find the target flight
                                    for (Flight f : list_of_flights) {
                                        if (f.getFlight_id() == Integer.parseInt(input[1])) {
                                            f.setNum_of_booked(0);
                                            f.setTicketPrice(f.getTicketPrice());
                                            // Departure day
                                            String dep_day = f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                                            // Departure time
                                            String dep_time = f.getDep_local_time().toString();
                                            // Print the flight information
                                            System.out.printf("Reset passengers booked to 0 for Flight %d, %s %s %s --> %s.\n",
                                                    Integer.parseInt(input[1]), dep_day, dep_time, f.getDep_location().getLocation_name(),
                                                    f.getArr_location().getLocation_name());
                                            break;
                                        }
                                    }
                                    break;
                                default:
                                    // If user's input is like this: "flight 0 0" hummmm yea
                                    boolean is_num = true;
                                    try {
                                        int num = Integer.parseInt(input[2]);
                                    } catch (NumberFormatException e) {
                                        is_num = false;
                                    }
                                    if (is_num && !input[1].equalsIgnoreCase("ADD") && !input[1].equalsIgnoreCase("IMPORT") &&
                                            !input[1].equalsIgnoreCase("EXPORT") && !input[0].equalsIgnoreCase("FLIGHTS")) {
                                        System.out.println("Invalid Flight ID.");
                                    }
                                    break;
                            }
                        }
                        break;
                    case ("LOCATIONS"):
                        // list all available locations in alphabetical order
                        // If no locations are available
                        if (list_of_locations.isEmpty()) {
                            System.out.println("Locations (0):");
                            System.out.println("(None)");
                            break;
                        }
                        ArrayList<Location> sorted_location = sort_locations(list_of_locations);
                        System.out.printf("Locations (%d):\n", sorted_location.size());
                        for (int i = 0; i < sorted_location.size() - 1; i++) {
                            System.out.print(sorted_location.get(i).getLocation_name() + ", ");
                        }
                        System.out.println(sorted_location.get(sorted_location.size() - 1).getLocation_name());
                        break;
                    case ("LOCATION"):
                        // No parameters given.
                        if (input.length == 1) {
                            System.out.println("Usage:\nLOCATION <name>\nLOCATION ADD <name> <latitude> <longitude> <demand_coefficient>\n" +
                                    "LOCATION IMPORT/EXPORT <filename>");
                            break;
                        }
                        // There are some commends under LOCATION
                        switch (input[1].toUpperCase()) {
                            // Add a location
                            case ("ADD"):
                                // Error checking
                                // 1) Not enough command arguments given
                                if (input.length < 6) {
                                    System.out.println("Usage:   LOCATION ADD <name> <lat> <long> <demand_coefficient>\n" +
                                            "Example: LOCATION ADD Sydney -33.847927 150.651786 0.2");
                                    break;
                                }
                                // 2) Location is already present in the database (case insensitive based on name)
                                if (locationExist(input[2], list_of_locations)) {
                                    System.out.println("This location already exists.");
                                    break;
                                }
                                // 3) Latitude exceeds bounds or is an invalid number
                                if (!Location.is_Valid_lat(input[3])) {
                                    System.out.println("Invalid latitude. It must be a number of degrees between -85 and +85.");
                                    break;
                                }
                                // 4) Longitude exceeds bounds or is an invalid number
                                if (!Location.is_Valid_lon(input[4])) {
                                    System.out.println("Invalid longitude. It must be a number of degrees between -180 and +180.");
                                    break;
                                }
                                // 5) Demand coefficient exceeds bounds or is an invalid number.
                                if (!Location.is_Valid_coe(input[5])) {
                                    System.out.println("Invalid demand coefficient. It must be a number between -1 and +1.");
                                    break;
                                }
                                // If no errors, we can add this location to the database
                                Location newLocation = new Location(input[2], Double.parseDouble(input[3]), Double.parseDouble(input[4]),
                                        Double.parseDouble(input[5]));
                                list_of_locations.add(newLocation);
                                System.out.printf("Successfully added location %s.\n", input[2]);
                                break;
                            // Import locations to csv file
                            case ("IMPORT"):
                                importLocations(input);
                                break;
                            // Export locations to csv file
                            case ("EXPORT"):
                                exportLocations(input);
                                break;
                            // View details about a location (it’s name, coordinates, demand coefficient)
                            default:
                                // Checking error
                                // Location is not present in the database (case insensitive based on name)
                                if (!locationExist(input[1], list_of_locations)) {
                                    System.out.println("Invalid location name.");
                                    break;
                                }
                                // Print the location list
                                Location location = is_location_exist(input[1], list_of_locations);
                                System.out.printf("Location:    %s\n", location.getLocation_name());
                                System.out.printf("Latitude:    %f\n", location.getLatitude());
                                System.out.printf("Longitude:   %f\n", location.getLongitude());
                                if (location.getDemand_coe() >= 0) {
                                    System.out.printf("Demand:      +%.4f\n", location.getDemand_coe());
                                } else {
                                    System.out.printf("Demand:      %.4f\n", location.getDemand_coe());
                                }
                                break;
                        }
                        break;
                    // list all departing and arriving flights, in order of the time they arrive/depart
                    case ("SCHEDULE"):
                        // Error checking
                        // 1) No parameters given.
                        if (input.length == 1) {
                            System.out.println("Usage:\nSCHEDULE <location_name>");
                            break;
                        }
                        // 2) Location is not present in the database (case insensitive based on name)
                        if (is_location_exist(input[1], list_of_locations) == null) {
                            System.out.println("This location does not exist in the system.");
                            break;
                        }
                        Location l = is_location_exist(input[1], list_of_locations);// Store the departing flight in this location
                        ArrayList<Flight> schedule = new ArrayList<>();
                        assert l != null;
                        schedule.addAll(l.getDeparting_f());
                        schedule.addAll(l.getArriving_f());

                        // Sort the flight schedule
                        ArrayList<Flight> newSchedule = sort_flights(schedule, l);
                        // Print the flight schedule
                        System.out.printf("%s\n", l.getLocation_name());
                        System.out.println("-------------------------------------------------------");
                        System.out.println("ID   Time        Departure/Arrival to/from Location");
                        System.out.println("-------------------------------------------------------");

                        for (Flight f : newSchedule) {
                            String dep_day = f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            String arr_day = Flight.arrival_day_week(f).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            String dep_time = f.getDep_local_time().toString();
                            String arr_time = Flight.arrival_l_time(f).toString();
                            if (f.getDep_location() == l) {
                                System.out.printf("%4d %s %s   Departure to %s\n", f.getFlight_id(), dep_day, dep_time,
                                        f.getArr_location().getLocation_name());
                            }
                            if (f.getArr_location() == l) {
                                System.out.printf("%4d %s %s   Arrival from %s\n", f.getFlight_id(), arr_day, arr_time,
                                        f.getDep_location().getLocation_name());
                            }
                        }
                        break;
                    case ("DEPARTURES"):
                        // Error checking
                        // 1) No parameters given.
                        if (input.length == 1) {
                            System.out.println("Usage:\nDEPARTURES <location_name>");
                            break;
                        }
                        // 2) Location is not present in the database (case insensitive based on name)
                        if (is_location_exist(input[1], list_of_locations) == null) {
                            System.out.println("This location does not exist in the system.");
                            break;
                        }
                        // Store the departing flight in this location
                        Location ll = is_location_exist(input[1], list_of_locations);
                        assert ll != null;
                        ArrayList<Flight> dep_schedule = new ArrayList<>(ll.getDeparting_f());
                        // Sort all departing flights, in order of departure time
                        ArrayList<Flight> sorted_dep_schedule = sort_dep(dep_schedule);
                        // Print the flight schedule
                        System.out.printf("%s\n", ll.getLocation_name());
                        System.out.println("-------------------------------------------------------");
                        System.out.println("ID   Time        Departure/Arrival to/from Location");
                        System.out.println("-------------------------------------------------------");

                        // Print the schedule
                        for (Flight f : sorted_dep_schedule) {
                            String dep_time = f.getDep_local_time().toString();
                            String dep_day = f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            System.out.printf("%4d %s %s   Departure to %s\n", f.getFlight_id(), dep_day, dep_time,
                                    f.getArr_location().getLocation_name());
                        }
                        break;
                    case ("ARRIVALS"):
                        // Error checking
                        // 1) No parameters given.
                        if (input.length == 1) {
                            System.out.println("Usage:\nARRIVALS <location_name>");
                            break;
                        }
                        // 2) Location is not present in the database (case insensitive based on name)
                        if (is_location_exist(input[1], list_of_locations) == null) {
                            System.out.println("This location does not exist in the system.");
                            break;
                        }
                        Location lll = is_location_exist(input[1], list_of_locations);
                        assert lll != null;
                        ArrayList<Flight> arr_schedule = new ArrayList<>(lll.getArriving_f());
                        // Sort all arriving flights, in order of arrival time
                        ArrayList<Flight> sorted_arr_schedule = sort_arr(arr_schedule);
                        System.out.printf("%s\n", lll.getLocation_name());
                        System.out.println("-------------------------------------------------------");
                        System.out.println("ID   Time        Departure/Arrival to/from Location");
                        System.out.println("-------------------------------------------------------");

                        // Print the schedule
                        for (Flight f : sorted_arr_schedule) {
                            String arr_day = Flight.arrival_day_week(f).getDisplayName(TextStyle.SHORT, Locale.getDefault());
                            String arr_time = Flight.arrival_l_time(f).toString();
                            System.out.printf("%4d %s %s   Arrival from %s\n", f.getFlight_id(), arr_day, arr_time,
                                    f.getDep_location().getLocation_name());
                        }
                        break;
                    case ("TRAVEL"):
                        // Error Checking
                        // No parameters given.
                        if (input.length < 3) {
                            System.out.println("Usage: TRAVEL <from> <to> [cost/duration/stopovers/layover/flight_time]");
                            break;
                        }
                        // Starting location is not present in the database (case insensitive based on name.
                        if (is_location_exist(input[1], list_of_locations) == null) {
                            System.out.println("Starting location not found.");
                            break;
                        }
                        // Ending location is not present in the database (case insensitive based on name.
                        if (is_location_exist(input[2], list_of_locations) == null) {
                            System.out.println("Ending location not found.");
                            break;
                        }
                        // Bad sorting property.
                        if (input.length > 3) {
                            if (!input[3].equalsIgnoreCase("cost") && !input[3].equalsIgnoreCase("duration")
                                    && !input[3].equalsIgnoreCase("stopovers") && !input[3].equalsIgnoreCase("layover")
                                    && !input[3].equalsIgnoreCase("flight_time")) {
                                System.out.println("Invalid sorting property: must be either cost, duration, stopovers, layover, or flight_time.");
                                break;
                            }
                        }

                        // Get the "from" and "to" location
                        Location from = is_location_exist(input[1], list_of_locations);
                        Location to = is_location_exist(input[2], list_of_locations);
                        // No flight paths of 3 stopovers or less are available from the given starting location to the ending destination.
                        if (from.getDeparting_f() == null || to.getArriving_f() == null) {
                            System.out.printf("Sorry, no flights with 3 or less stopovers are available from %s to %s.\n",
                                    from.getLocation_name(), to.getLocation_name());
                            break;
                        }
                        // To store the travel routes
                        ArrayList<ArrayList<Flight>> routes = new ArrayList<>();
                        // Count the possible routes
                        int count = 0;
                        // Loop through all the departing flight in the start location
                        for (Flight f1 : from.getDeparting_f()) {
                            // Now check if any flight at the start location can arrive at the end location directly
                            // Loop through all the arriving flights in the end location
                            for (Flight f2 : to.getArriving_f()) {
                                // If f1 and f2 is the same flight, add it to the targeted flights list and route list (0 stopover) Lovely and simple aww
                                if (f1 == f2) {
                                    // To store the individual targeted flights
                                    ArrayList<Flight> flights = new ArrayList<>();
                                    flights.add(f1);
                                    routes.add(flights);
                                    count++;
                                }
                                // Now check if there are other travel plans with stopovers
                                // First check if the f1's arriving place is the departing place for f2 (one stopover)
                                if (f1.getArr_location() == f2.getDep_location() && f2.getArr_location() != from && f1.getArr_location() != to) {
                                    // add f1 and f2 to the target flights list and add the whole list to route
                                    if (f2.getArr_location() == to) {
                                        // To store the individual targeted flights
                                        ArrayList<Flight> flights = new ArrayList<>();
                                        flights.add(f1);
                                        flights.add(f2);
                                        routes.add(flights);
                                        count++;
                                    }
                                }
                                // Secondly check if there's a f3, which its departing location is f1's arriving location
                                // and its arriving location is f2's departing location. (two stopovers)
                                for (Flight f3 : f1.getArr_location().getDeparting_f()) {
                                    if (f1.getArr_location() == f3.getDep_location() && f3.getArr_location() == f2.getDep_location() &&
                                            f3.getArr_location() != from && f3.getArr_location() != to && f1.getArr_location() != to) {
                                        // add f1, f2, f3 to the target flights list and add the whole list to route
                                        if (f2.getArr_location() == to) {
                                            // To store the individual targeted flights
                                            ArrayList<Flight> flights = new ArrayList<>();
                                            flights.add(f1);
                                            flights.add(f3);
                                            flights.add(f2);
                                            routes.add(flights);
                                            count++;
                                        }
                                    }
                                    // Thirdly check if there's a f4 which it's departing location is f3's arriving location,
                                    // it's arriving location is f2's departing location (three stopovers)
                                    for (Flight f4 : list_of_flights) {
                                        if (f1.getArr_location() == f3.getDep_location() && f3.getArr_location() == f4.getDep_location() &&
                                                f3.getArr_location() != from && f4.getArr_location() == f2.getDep_location() &&
                                                f4.getArr_location() != from && f4.getArr_location() != f3.getDep_location() &&
                                                f3.getArr_location() != to && f4.getArr_location() != to && f1.getArr_location() != to) {
                                            if (f2.getArr_location() == to) {
                                                // To store the individual targeted flights
                                                ArrayList<Flight> flights = new ArrayList<>();
                                                flights.add(f1);
                                                flights.add(f3);
                                                flights.add(f4);
                                                flights.add(f2);
                                                routes.add(flights);
                                                count++;
                                            }
                                        }
                                        if (f1.getArr_location() == f4.getDep_location() && f4.getArr_location() == f3.getDep_location() &&
                                                f4.getArr_location() != from && f3.getArr_location() == f2.getDep_location() &&
                                                f3.getArr_location() != from && f3.getArr_location() != f4.getDep_location() &&
                                                f4.getArr_location() != to && f3.getArr_location() != to && f1.getArr_location() != to) {
                                            if (f2.getArr_location() == to) {
                                                // To store the individual targeted flights
                                                ArrayList<Flight> flights = new ArrayList<>();
                                                flights.add(f1);
                                                flights.add(f4);
                                                flights.add(f3);
                                                flights.add(f2);
                                                routes.add(flights);
                                                count++;
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        // No flight paths of 3 stopovers or less are available from the given starting location to the ending destination.
                        if (count == 0) {
                            System.out.printf("Sorry, no flights with 3 or less stopovers are available from %s to %s.\n",
                                    from.getLocation_name(), to.getLocation_name());
                            break;
                        }
                        // Now we can print the required route
                        if (input.length == 3) {
                            // TRAVEL <from> <to>
                            // Default ordering: shortest overall duration
                            // Store the total duration of each route
                            ArrayList<Integer> total_duration = new ArrayList<>();
                            // Get the total duration of each route
                            for (ArrayList<Flight> f_route : routes) {
                                int duration = 0;
                                for (Flight f : f_route) {
                                    duration = duration + f.getDuration();
                                }
                                // Add layover time
                                for (int k = 0; k < f_route.size() - 1; k++) {
                                    duration = duration + Flight.layover(f_route.get(k), f_route.get(k + 1));
                                }
                                total_duration.add(duration);
                            }
                            // Compare the total duration and print the shortest one
                            int shortest = total_duration.get(0);
                            int index = 0;
                            if (total_duration.size() > 1) {
                                for (int i = 0; i < total_duration.size(); i++) {
                                    if (shortest > total_duration.get(i)) {
                                        shortest = total_duration.get(i);
                                        index = i;
                                    }
                                }
                            }
                            print_for_travel_two(routes, index);
                        }
                        if (input.length > 3) {
                            // 1) sort by cost - minimum current cost
                            if (input[3].equalsIgnoreCase("cost")) {
                                // Get the total cost of each route
                                // Then sort the list by cost
                                // If total cost is equal, sort then by minimum total duration.
                                routes.sort((o1, o2) -> {
                                    // Get cost
                                    double cost1 = 0;
                                    for (Flight f1 : o1) {
                                        cost1 = cost1 + f1.getTicketPrice();
                                    }
                                    double cost2 = 0;
                                    for (Flight f2 : o2) {
                                        cost2 = cost2 + f2.getTicketPrice();
                                    }
                                    // Get duration
                                    int duration1 = 0;
                                    for (Flight f1 : o1) {
                                        duration1 = duration1 + f1.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o1.size() - 1; k++) {
                                        duration1 = duration1 + Flight.layover(o1.get(k), o1.get(k + 1));
                                    }
                                    int duration2 = 0;
                                    for (Flight f2 : o2) {
                                        duration2 = duration2 + f2.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o2.size() - 1; k++) {
                                        duration2 = duration2 + Flight.layover(o2.get(k), o2.get(k + 1));
                                    }
                                    if (cost1 == cost2) {
                                        return duration1 - duration2;
                                    } else {
                                        return (int) (cost1 - cost2);
                                    }
                                });
                                int index = 0;
                                // If n is provided
                                if (input.length > 4) {
                                    // If n is larger than the number of flights available, display the last one in the ordering.
                                    if (Integer.parseInt(input[4]) + 1 > count) {
                                        index = count - 1;
                                    } else if (Integer.parseInt(input[4]) < 0) {
                                        index = 0;
                                    } else {
                                        index = Integer.parseInt(input[4]);
                                    }
                                }
                                print_for_travel_two(routes, index);
                            }
                            // 2) sort by duration - minimum total duration
                            if (input[3].equalsIgnoreCase("duration")) {
                                // Get the total duration of each route
                                // Then sort the list by duration
                                // If total duration is equal, sort then by minimum current cost.
                                routes.sort((o1, o2) -> {
                                    // Get cost
                                    double cost1 = 0;
                                    for (Flight f1 : o1) {
                                        cost1 = cost1 + f1.getTicketPrice();
                                    }
                                    double cost2 = 0;
                                    for (Flight f2 : o2) {
                                        cost2 = cost2 + f2.getTicketPrice();
                                    }
                                    // Get duration
                                    int duration1 = 0;
                                    for (Flight f1 : o1) {
                                        duration1 = duration1 + f1.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o1.size() - 1; k++) {
                                        duration1 = duration1 + Flight.layover(o1.get(k), o1.get(k + 1));
                                    }
                                    int duration2 = 0;
                                    for (Flight f2 : o2) {
                                        duration2 = duration2 + f2.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o2.size() - 1; k++) {
                                        duration2 = duration2 + Flight.layover(o2.get(k), o2.get(k + 1));
                                    }
                                    if (duration1 == duration2) {
                                        return (int) (cost1 - cost2);
                                    } else {
                                        return duration1 - duration2;
                                    }
                                });
                                int index = 0;
                                // If n is provided
                                if (input.length > 4) {
                                    // If n is larger than the number of flights available, display the last one in the ordering.
                                    if (Integer.parseInt(input[4]) + 1 > count) {
                                        index = count - 1;
                                    } else if (Integer.parseInt(input[4]) < 0) {
                                        index = 0;
                                    } else {
                                        index = Integer.parseInt(input[4]);
                                    }
                                }
                                print_for_travel_two(routes, index);
                            }
                            // sort by stopovers - minimum stopovers
                            if (input[3].equalsIgnoreCase("stopovers")) {
                                // Get the total stopovers of each route
                                // Then sort the list by number of stopovers
                                // If the number of stopovers are equal, sort then by minimum total duration (and then by minimum cost).
                                routes.sort((o1, o2) -> {
                                    // Get cost
                                    double cost1 = 0;
                                    for (Flight f1 : o1) {
                                        cost1 = cost1 + f1.getTicketPrice();
                                    }
                                    double cost2 = 0;
                                    for (Flight f2 : o2) {
                                        cost2 = cost2 + f2.getTicketPrice();
                                    }
                                    // Get duration
                                    int duration1 = 0;
                                    for (Flight f1 : o1) {
                                        duration1 = duration1 + f1.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o1.size() - 1; k++) {
                                        duration1 = duration1 + Flight.layover(o1.get(k), o1.get(k + 1));
                                    }
                                    int duration2 = 0;
                                    for (Flight f2 : o2) {
                                        duration2 = duration2 + f2.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o2.size() - 1; k++) {
                                        duration2 = duration2 + Flight.layover(o2.get(k), o2.get(k + 1));
                                    }
                                    // Get stopovers
                                    int stopovers1 = o1.size() - 1;
                                    int stopovers2 = o2.size() - 1;
                                    if (stopovers1 == stopovers2) {
                                        if (duration1 == duration2) {
                                            return (int) (cost1 - cost2);
                                        } else {
                                            return duration1 - duration2;
                                        }
                                    } else {
                                        return stopovers1 - stopovers2;
                                    }
                                });
                                int index = 0;
                                // If n is provided
                                if (input.length > 4) {
                                    // If n is larger than the number of flights available, display the last one in the ordering.
                                    if (Integer.parseInt(input[4]) + 1 > count) {
                                        index = count - 1;
                                    } else if (Integer.parseInt(input[4]) < 0) {
                                        index = 0;
                                    } else {
                                        index = Integer.parseInt(input[4]);
                                    }
                                }
                                print_for_travel_two(routes, index);
                            }
                            // sort by layover - minimum layover time
                            if (input[3].equalsIgnoreCase("layover")) {
                                // Get the layover time of each route
                                // Then sort the list by layover time
                                // If the layover time are equal, sort then by minimum total duration (and then by minimum cost).
                                routes.sort((o1, o2) -> {
                                    // Get cost
                                    double cost1 = 0;
                                    for (Flight f1 : o1) {
                                        cost1 = cost1 + f1.getTicketPrice();
                                    }
                                    double cost2 = 0;
                                    for (Flight f2 : o2) {
                                        cost2 = cost2 + f2.getTicketPrice();
                                    }
                                    // Get duration
                                    int duration1 = 0;
                                    for (Flight f1 : o1) {
                                        duration1 = duration1 + f1.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o1.size() - 1; k++) {
                                        duration1 = duration1 + Flight.layover(o1.get(k), o1.get(k + 1));
                                    }
                                    int duration2 = 0;
                                    for (Flight f2 : o2) {
                                        duration2 = duration2 + f2.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o2.size() - 1; k++) {
                                        duration2 = duration2 + Flight.layover(o2.get(k), o2.get(k + 1));
                                    }
                                    // Get layover time
                                    int layover1 = 0;
                                    int layover2 = 0;
                                    for (int a = 0; a < o1.size() - 1; a++) {
                                        layover1 = layover1 + Flight.layover(o1.get(a), o1.get(a + 1));
                                    }
                                    for (int b = 0; b < o1.size() - 1; b++) {
                                        layover2 = layover2 + Flight.layover(o1.get(b), o1.get(b + 1));
                                    }
                                    if (layover1 == layover2) {
                                        if (duration1 == duration2) {
                                            return (int) (cost1 - cost2);
                                        } else {
                                            return duration1 - duration2;
                                        }
                                    } else {
                                        return layover1 - layover2;
                                    }
                                });
                                int index = 0;
                                // If n is provided
                                if (input.length > 4) {
                                    // If n is larger than the number of flights available, display the last one in the ordering.
                                    if (Integer.parseInt(input[4]) + 1 > count) {
                                        index = count - 1;
                                    } else if (Integer.parseInt(input[4]) < 0) {
                                        index = 0;
                                    } else {
                                        index = Integer.parseInt(input[4]);
                                    }
                                }
                                print_for_travel_two(routes, index);
                            }
                            // sort by flight_time - minimum flight time
                            if (input[3].equalsIgnoreCase("flight_time")) {
                                // Get the flight time of each route
                                // Then sort the list by flight time
                                // If flight time is equal, sort then by minimum total duration (and then by minimum cost).
                                routes.sort((o1, o2) -> {
                                    // Get cost
                                    double cost1 = 0;
                                    for (Flight f1 : o1) {
                                        cost1 = cost1 + f1.getTicketPrice();
                                    }
                                    double cost2 = 0;
                                    for (Flight f2 : o2) {
                                        cost2 = cost2 + f2.getTicketPrice();
                                    }
                                    // Get duration
                                    int duration1 = 0;
                                    for (Flight f1 : o1) {
                                        duration1 = duration1 + f1.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o1.size() - 1; k++) {
                                        duration1 = duration1 + Flight.layover(o1.get(k), o1.get(k + 1));
                                    }
                                    int duration2 = 0;
                                    for (Flight f2 : o2) {
                                        duration2 = duration2 + f2.getDuration();
                                    }
                                    // Add layover time
                                    for (int k = 0; k < o2.size() - 1; k++) {
                                        duration2 = duration2 + Flight.layover(o2.get(k), o2.get(k + 1));
                                    }
                                    // Get flight time
                                    int flight1 = 0;
                                    int flight2 = 0;
                                    for (Flight ff1 : o1) {
                                        flight1 = flight1 + ff1.getDuration();
                                    }
                                    for (Flight ff2 : o2) {
                                        flight2 = flight2 + ff2.getDuration();
                                    }
                                    if (flight1 == flight2) {
                                        if (duration1 == duration2) {
                                            return (int) (cost1 - cost2);
                                        } else {
                                            return duration1 - duration2;
                                        }
                                    } else {
                                        return flight1 - flight2;
                                    }
                                });
                                int index = 0;
                                // If n is provided
                                if (input.length > 4) {
                                    // If n is larger than the number of flights available, display the last one in the ordering.
                                    if (Integer.parseInt(input[4]) + 1 > count) {
                                        index = count - 1;
                                    } else if (Integer.parseInt(input[4]) < 0) {
                                        index = 0;
                                    } else {
                                        index = Integer.parseInt(input[4]);
                                    }
                                }
                                print_for_travel_two(routes, index);
                            }
                        }
                        break;
                    // Print the help menu for user
                    case ("HELP"):
                        System.out.println("FLIGHTS - list all available flights ordered by departure time, then departure location name");
                        System.out.println("FLIGHT ADD <departure time> <from> <to> <capacity> - add a flight");
                        System.out.println("FLIGHT IMPORT/EXPORT <filename> - import/export flights to csv file");
                        System.out.println("FLIGHT <id> - view information about a flight (from->to, departure arrival times, " +
                                "current ticket price, capacity, passengers booked)");
                        System.out.println("FLIGHT <id> BOOK <num> - book a certain number of passengers for the flight at the current ticket price, " +
                                "and then adjust the ticket price to reflect the reduced capacity remaining. " +
                                "If no number is given, book 1 passenger. If the given number of bookings is more than the remaining capacity, " +
                                "only accept bookings until the capacity is full.");
                        System.out.println("FLIGHT <id> REMOVE - remove a flight from the schedule");
                        System.out.println("FLIGHT <id> RESET - reset the number of passengers booked to 0, and the ticket price to its original state.\n");
                        System.out.println("LOCATIONS - list all available locations in alphabetical order");
                        System.out.println("LOCATION ADD <name> <lat> <long> <demand_coefficient> - add a location");
                        System.out.println("LOCATION <name> - view details about a location (it's name, coordinates, demand coefficient)");
                        System.out.println("LOCATION IMPORT/EXPORT <filename> - import/export locations to csv file");
                        System.out.println("SCHEDULE <location_name> - list all departing and arriving flights, in order of the time they arrive/depart");
                        System.out.println("DEPARTURES <location_name> - list all departing flights, in order of departure time");
                        System.out.println("ARRIVALS <location_name> - list all arriving flights, in order of arrival time\n");
                        System.out.println("TRAVEL <from> <to> [sort] [n] - list the nth possible flight route between a starting location and destination, " +
                                "with a maximum of 3 stopovers. Default ordering is for shortest overall duration. " +
                                "If n is not provided, display the first one in the order. " +
                                "If n is larger than the number of flights available, display the last one in the ordering.\n");
                        System.out.println("can have other orderings:");
                        System.out.println("TRAVEL <from> <to> cost - minimum current cost");
                        System.out.println("TRAVEL <from> <to> duration - minimum total duration");
                        System.out.println("TRAVEL <from> <to> stopovers - minimum stopovers");
                        System.out.println("TRAVEL <from> <to> layover - minimum layover time");
                        System.out.println("TRAVEL <from> <to> flight_time - minimum flight time\n");
                        System.out.println("HELP - outputs this help string.");
                        System.out.println("EXIT - end the program.");
                        break;

                    case ("EXIT"):
                        System.out.println("Application closed.");
                        end = 1;
                        break;

                    default:
                        System.out.println("Invalid command. Type 'help' for a list of commands.");
                        break;
                }
            }
            if (end != 1) {
                System.out.print("\nUser: ");
            }
        }

    }


    // Add a flight to the database
    // handle error cases and return status negative if error
    // (different status codes for different messages)
    // do not print out anything in this function
    public int addFlight(String date1, String date2, String start, String end, String capacity, int booked) {
        // Check error cases
        // 1) Time added was not in the correct format, for example “Monday 18:00”
        if (!is_valid_day(date1)) {
            return -1;
        }
        if (!is_valid_time(date2)) {
            return -8;
        }
        // 2) Starting location was not in the database.
        if (is_location_exist(start, list_of_locations) == null) {
            return -2;
        }
        Location dep_l = is_location_exist(start, list_of_locations);
        // 3) Ending location was not in the database.
        if (is_location_exist(end, list_of_locations) == null) {
            return -3;
        }
        Location arr_l = is_location_exist(end, list_of_locations);

        // 4) Capacity was not a positive integer.
        if (Integer.parseInt(capacity) <= 0) {
            return -4;
        }
        // 5) The two locations entered were the same
        if (start.equalsIgnoreCase(end)) {
            return -5;
        }
        Flight newFlight = new Flight(id_counter, date1 + " " + date2,
                dep_l, arr_l, Integer.parseInt(capacity));
        // 6) No runways are available for this flight at the designated location at that time.
        assert dep_l != null;
        if (dep_l.hasRunwayDepartureSpace(newFlight) != null) {
            return -6;
        }
        // 7) No runways are available for this flight at the designated location at that time.
        assert arr_l != null;
        if (arr_l.hasRunwayArrivalSpace(newFlight) != null) {
            return -7;
        }
        // Add new flight
        list_of_flights.add(newFlight);
        newFlight.setNum_of_booked(booked);
        id_counter = id_counter + 1;
        return 1;
    }

    // Add a location to the database
    // do not print out anything in this function
    // return negative numbers for error cases
    public int addLocation(String name, String lat, String lon, String demand) {
        // First check error cases
        try {
            Double.parseDouble(lat);
            Double.parseDouble(lon);
        } catch (NumberFormatException e) {
            return -5;
        }
        // 1) Location is already present in the database (case insensitive based on name)
        if (locationExist(name, list_of_locations)) {
            return -1;
        }
        // 2) Longitude exceeds bounds or is an invalid number.
        if (!Location.is_Valid_lon(lon)) {
            return -2;
        }
        // 3) Latitude exceeds bounds or is an invalid number
        if (!Location.is_Valid_lat(lat)) {
            return -3;
        }
        // 4) Demand coefficient exceeds bounds or is an invalid number.
        if (!Location.is_Valid_coe(demand)) {
            return -4;
        }
        // If no errors, add the location to the database
        Location newLocation = new Location(name, Double.parseDouble(lat), Double.parseDouble(lon),
                Double.parseDouble(demand));
        list_of_locations.add(newLocation);
        return 1;
    }

    //flight import <filename>
    public void importFlights(String[] command) {
        try {
            if (command.length < 3) throw new FileNotFoundException();
            BufferedReader br = new BufferedReader(new FileReader(new File(command[2])));
            String line;
            int count = 0;
            int err = 0;

            while ((line = br.readLine()) != null) {
                String[] lparts = line.split(",");
                if (lparts.length < 5) continue;
                String[] dparts = lparts[0].split(" ");
                if (dparts.length < 2) continue;
                int booked = 0;

                try {
                    booked = Integer.parseInt(lparts[4]);

                } catch (NumberFormatException e) {
                    continue;
                }

                int status = addFlight(dparts[0], dparts[1], lparts[1], lparts[2], lparts[3], booked);
                if (status < 0) {
                    err++;
                    continue;
                }
                count++;
            }
            br.close();
            System.out.println("Imported " + count + " flight" + (count != 1 ? "s" : "") + ".");
            if (err > 0) {
                if (err == 1) System.out.println("1 line was invalid.");
                else System.out.println(err + " lines were invalid.");
            }
        } catch (IOException e) {
            System.out.println("Error reading file.");
            return;
        }
    }

    // flight export <filename>
    // The method of writing to a file is taken from w3schools
    // https://www.w3schools.com/java/java_files_create.asp
    public void exportFlights(String[] command) {
        try {
            if (command.length < 3) throw new FileNotFoundException();
            // Create the file if not exist
            File flight_file = new File(command[2]);
            flight_file.createNewFile();
            int counter = 0;
            FileWriter F_file = new FileWriter(command[2], true);
            for (Flight f : list_of_flights) {
                F_file.write(f.flight_toString());
                counter++;
            }
            F_file.close();
            System.out.println("Exported " + counter + " flight" + (counter != 1 ? "s" : "") + ".");
        } catch (IOException e) {
            System.out.println("Error reading file.");
            return;
        }
    }

    //location import <filename>
    public void importLocations(String[] command) {
        try {
            if (command.length < 3) throw new FileNotFoundException();
            BufferedReader br = new BufferedReader(new FileReader(new File(command[2])));
            String line;
            int count = 0;
            int err = 0;

            while ((line = br.readLine()) != null) {
                String[] lparts = line.split(",");
                if (lparts.length < 4) continue;

                int status = addLocation(lparts[0], lparts[1], lparts[2], lparts[3]);
                if (status < 0) {
                    err++;
                    continue;
                }
                count++;
            }
            br.close();
            System.out.println("Imported " + count + " location" + (count != 1 ? "s" : "") + ".");
            if (err > 0) {
                if (err == 1) System.out.println("1 line was invalid.");
                else System.out.println(err + " lines were invalid.");
            }

        } catch (IOException e) {
            System.out.println("Error writing file.");
            return;
        }
    }

    // location export <filename>
    // The method of writing to a file is taken from w3schools
    // https://www.w3schools.com/java/java_files_create.asp
    public void exportLocations(String[] command) {
        try {
            int counter = 0;
            if (command.length < 3) throw new FileNotFoundException();
            File location_file = new File(command[2]);
            location_file.createNewFile();
            FileWriter L_file = new FileWriter(command[2], true);
            for (Location l : list_of_locations) {
                L_file.write(l.location_toString());
                counter++;
            }
            L_file.close();
            System.out.println("Exported " + counter + " location" + (counter != 1 ? "s" : "") + ".");
        } catch (IOException e) {
            System.out.println("Error reading file.");
            return;
        }
    }

    // Error case checking!
    // 1) Time added was not in the correct format, for example “Monday 18:00”
    public static boolean is_valid_day(String day) {
        boolean is_valid = false;
        if (day.equalsIgnoreCase("Monday") || day.equalsIgnoreCase("Tuesday")
                || day.equalsIgnoreCase("Wednesday") || day.equalsIgnoreCase("Thursday")
                || day.equalsIgnoreCase("Friday") || day.equalsIgnoreCase("Saturday")
                || day.equalsIgnoreCase("Sunday")) {
            is_valid = true;
        }
        return is_valid;
    }

    public static boolean is_valid_time(String time) {
        boolean is_valid = true;
        // The method of checking if time format is correct is taken from StackOverflow
        // https://stackoverflow.com/questions/40940991/how-to-check-if-time-format-is-correct-in-java-with-exception
        DateTimeFormatter time_Formatter = DateTimeFormatter.ofPattern("H:mm")
                .withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalTime.parse(time, time_Formatter);
        } catch (DateTimeParseException | NullPointerException e) {
            is_valid = false;
        }
        return is_valid;
    }

    // 2) Check if the location was in the database.
    public static Location is_location_exist(String name, ArrayList<Location> location_list) {
        for (Location l : location_list) {
            if (name.equalsIgnoreCase(l.getLocation_name())) {
                return l;
            }
        }
        return null;
    }

    // 3) Flight ID was not in the database
    public static boolean is_id_exist(int id, ArrayList<Flight> flight_list) {
        try {
            for (Flight f : flight_list) {
                if (id == f.getFlight_id()) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    // 4) Location is already present in the database (case insensitive based on name)
    public static boolean locationExist(String name, ArrayList<Location> location_list) {
        for (Location l : location_list) {
            if (name.equalsIgnoreCase(l.getLocation_name())) {
                return true;
            }
        }
        return false;
    }

    // Helper functions

    // 1) Sort flight list ordered by departure time, then departure location name
    // The method of sorting an array list is taken from StackOverflow & a little bit help from intelliJ I guess...
    // https://stackoverflow.com/questions/14154127/collections-sortlistt-comparator-super-t-method-example
    public static ArrayList<Flight> sorted(ArrayList<Flight> sorted) {
        sorted.sort((o1, o2) -> {
            if (o1.getDep_dayofWeek().compareTo(o2.getDep_dayofWeek()) == 0) {
                if (o1.getDep_local_time().compareTo(o2.getDep_local_time()) == 0) {
                    String o1_depLocation = o1.getDep_location().getLocation_name().toLowerCase();
                    String o2_depLocation = o2.getDep_location().getLocation_name().toLowerCase();
                    return o1_depLocation.charAt(0) - o2_depLocation.charAt(0);
                } else {
                    return o1.getDep_local_time().compareTo(o2.getDep_local_time());
                }
            } else {
                return o1.getDep_dayofWeek().compareTo(o2.getDep_dayofWeek());
            }
        });
        return sorted;
    }

    // 2）Sort location list in alphabetical order
    public static ArrayList<Location> sort_locations(ArrayList<Location> sorted) {
        sorted.sort((o1, o2) -> {
            String o1_location = o1.getLocation_name();
            String o2_location = o2.getLocation_name();
            return o1_location.compareToIgnoreCase(o2_location);
        });
        return sorted;
    }

    // 3) Sort all departing and arriving flights, in order of the time they arrive/depart
    public static ArrayList<Flight> sort_flights(ArrayList<Flight> sorted, Location l) {
        sorted.sort((o1, o2) -> {
            DayOfWeek o1_day = null;
            LocalTime o1_time = null;
            DayOfWeek o2_day = null;
            LocalTime o2_time = null;
            if (o1.getArr_location() == l) {
                o1_day = Flight.arrival_day_week(o1);
                o1_time = Flight.arrival_l_time(o1);
            }
            if (o2.getArr_location() == l) {
                o2_day = Flight.arrival_day_week(o2);
                o2_time = Flight.arrival_l_time(o2);
            }
            if (o1.getDep_location() == l) {
                o1_day = o1.getDep_dayofWeek();
                o1_time = o1.getDep_local_time();
            }
            if (o2.getDep_location() == l) {
                o2_day = o2.getDep_dayofWeek();
                o2_time = o2.getDep_local_time();
            }
            if (o1_day == o2_day) {
                return o1_time.compareTo(o2_time);
            } else {
                return o1_day.compareTo(o2_day);
            }

        });
        return sorted;
    }

    // 4) Sort all departing flights, in order of departure time
    public static ArrayList<Flight> sort_dep(ArrayList<Flight> sorted) {
        sorted.sort((o1, o2) -> {
            if (o1.getDep_dayofWeek().compareTo(o2.getDep_dayofWeek()) == 0) {
                return o1.getDep_local_time().compareTo(o2.getDep_local_time());
            } else {
                return o1.getDep_dayofWeek().compareTo(o2.getDep_dayofWeek());
            }
        });
        return sorted;
    }

    // 5) Sort all arriving flights, in order of arrival time
    public static ArrayList<Flight> sort_arr(ArrayList<Flight> sorted) {
        sorted.sort((o1, o2) -> {
            if (Flight.arrival_day_week(o1).compareTo(Flight.arrival_day_week(o2)) == 0) {
                return Flight.arrival_l_time(o1).compareTo(Flight.arrival_l_time(o2));
            } else {
                return Flight.arrival_day_week(o1).compareTo(Flight.arrival_day_week(o2));
            }
        });
        return sorted;
    }

    // 6) This is for printing -- "Travel" command
    public static void print_for_travel() {
        System.out.println("-------------------------------------------------------------");
        System.out.println("ID   Cost      Departure   Arrival     Source --> Destination");
        System.out.println("-------------------------------------------------------------");
    }

    public static void print_for_travel_two(ArrayList<ArrayList<Flight>> routes, int index) {
        // get the duration
        int total_duration = 0;
        for (Flight f : routes.get(index)) {
            total_duration = total_duration + f.getDuration();
        }
        // Add layover time
        for (int k = 0; k < routes.get(index).size() - 1; k++) {
            total_duration = total_duration + Flight.layover(routes.get(index).get(k), routes.get(index).get(k + 1));
        }
        int h = total_duration / 60;
        int m = total_duration % 60;
        double cost = 0;
        // Get total cost
        for (Flight f1 : routes.get(index)) {
            cost = cost + f1.getTicketPrice();
        }
        System.out.printf("Legs:             %d\n", routes.get(index).size());
        System.out.printf("Total Duration:   %dh %dm\n", h, m);
        System.out.printf("Total Cost:       $%.2f\n", cost);
        print_for_travel();
        int counter = 1;
        for (int j = 0; j < routes.get(index).size(); j++) {
            if (counter < routes.get(index).size()) {
                print_detail(routes.get(index).get(j));
                int layover_time = Flight.layover(routes.get(index).get(j), routes.get(index).get(j + 1));
                int hours = layover_time / 60;
                int minuets = layover_time % 60;
                System.out.printf("LAYOVER %dh %dm at %s\n", hours, minuets,
                        routes.get(index).get(j).getArr_location().getLocation_name());
                counter++;
            } else {
                print_detail(routes.get(index).get(j));
            }
        }
    }

    public static void print_detail(Flight f) {
        System.out.printf("%4d $%8.2f %s %s   %s %s   %s --> %s\n", f.getFlight_id(),
                f.getTicketPrice(), f.getDep_dayofWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                f.getDep_local_time().toString(), Flight.arrival_day_week(f).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                Flight.arrival_l_time(f).toString(), f.getDep_location().getLocation_name(), f.getArr_location().getLocation_name());
    }

}