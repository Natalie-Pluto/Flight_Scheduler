import java.lang.*;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class Flight {
    private int flight_id;
    private String dep_time;
    private DayOfWeek dep_dayofWeek;
    private LocalTime dep_local_time;
    private Location dep_location;
    private Location arr_location;
    private int capacity;
    private double ticketPrice;
    private int num_of_booked;

    // Create a constructor
    public Flight (int flight_id, String dep_time, Location dep_location, Location arr_location, int capacity) {
        DateTimeFormatter time_Formatter = DateTimeFormatter.ofPattern("H:mm")
                .withResolverStyle(ResolverStyle.STRICT);
        this.flight_id = flight_id;
        this.dep_time = dep_time;
        this.dep_dayofWeek = DayOfWeek.valueOf((dep_time.split(" ")[0]).toUpperCase());
        this.dep_local_time = LocalTime.parse(dep_time.split(" ")[1], time_Formatter);
        this.dep_location = dep_location;
        this.arr_location = arr_location;
        this.capacity = capacity;
        this.num_of_booked = 0;
    }

    //get the number of minutes this flight takes (round to nearest whole number)
    public int getDuration() {
        // Get the distance between the start and end locations by calling the getDistance() method
        double distance = getDistance();
        // Assuming the average speed of an aircraft is 720km/h.
        int speed = 720/60;
        // Do the calculation (To the nearest whole num!) :)
        int duration = (int) Math.round(distance/speed);
        return duration;
    }

    // Get the local time of the arriving time
    public static LocalTime arrival_l_time(Flight f) {
        LocalTime arr_local_time = f.dep_local_time.plusMinutes(f.getDuration());
        return arr_local_time;
    }

    // Get the day of week of the arriving time
    public static DayOfWeek arrival_day_week(Flight f) {
        DayOfWeek arr_day_of_week;
        int value = arrival_l_time(f).compareTo(f.dep_local_time);
        // If the arrival time is in the next day:
        if(value < 0) {
            // If it's departure time was on Sunday...
            if (f.dep_dayofWeek.getValue() == 7) {
                arr_day_of_week = DayOfWeek.valueOf("MONDAY");
            }
            else {
                arr_day_of_week = DayOfWeek.of((f.dep_dayofWeek.getValue() + 1));
            }
        }
        // If the arrival time is not in the next day:
        else {
            arr_day_of_week = f.dep_dayofWeek;
        }
        return arr_day_of_week;
    }

    //implement the ticket price formula
    public double getTicketPrice() {
        if (num_of_booked == 0) {
            ticketPrice = (30 + 4 * (arr_location.getDemand_coe() - dep_location.getDemand_coe())) * (getDistance()/100);
            setTicketPrice(ticketPrice);
            return ticketPrice;
        }
        // The demand coefficient differential between locations
        double diff_coe = arr_location.getDemand_coe() - dep_location.getDemand_coe();
        // Proportion of seats filled (booked/capacity)
        double x = (double) num_of_booked/capacity;
        // Multiplier for ticket price to determine current value
        double y = 0;
        // Compare the coes and get the right value of "y"
        if (x > 0 && x <= 0.5) {
            y = -0.4 * x + 1;
        } else if (x > 0.5 && x <= 0.7) {
            y = x + 0.3;
        } else if (x > 0.7 && x <= 1) {
            y = (0.2/Math.PI) * Math.atan(20 * x - 14) + 1;
        }
        // Now we can finally start to calculate the ticket price!! ($v$)
        ticketPrice = y * (getDistance()/100) * (30 + 4 * diff_coe);
        setTicketPrice(ticketPrice);
        return ticketPrice;
    }

    // Setter for ticket price
    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    //book the given number of passengers onto this flight, returning the total cost
    public double book(int num) {
        int counter = num;
        int num_booked = getNum_of_booked();
        double price = 0;
        while (counter > 0 && num_booked <= capacity) {
            // Add the cost
            price = price + getTicketPrice();
            // Increase the number of passengers booked
            num_booked =num_booked + 1;
            setNum_of_booked(num_booked);
            counter = counter - 1;
        }
        return price;
    }

    //get the distance of this flight in km
    public double getDistance() {
        // Call the helper function in Location class
        double f_distance = Location.distance(dep_location, arr_location);
        return f_distance;
    }


    // Get the days between two flights
    public static int Days_between(DayOfWeek d1, DayOfWeek d2) {
        // Calculates the days between to flights
        if(d1.getValue() == 7 && d2.getValue() == 1) {
            return 1;
        }
        if(d2.getValue() < d1.getValue() && d2.getValue() != 1 && d1.getValue() != 7) {
            return (Math.abs(d2.getValue() - d1.getValue()) + 7);
        }
        return Math.abs(d2.getValue() - d1.getValue());
    }
    // Get the days between two flights (For has runway space)
    public static int between(DayOfWeek d1, DayOfWeek d2) {
        // Calculates the days between to flights
        if((d1.getValue() == 7 && d2.getValue() == 1) || (d1.getValue() == 1 && d2.getValue() == 7)) {
            return 1;
        }
        return Math.abs(d2.getValue() - d1.getValue());
    }

    //get the layover time, in minutes, between two flights
    public static int time_between(LocalTime t1, LocalTime  t2, DayOfWeek d1, DayOfWeek d2) {
        int layover_time = 0;
        // Get the days between two flights
        int days_between = between(d1,d2);
        // Calculate the amount of layover time (in minutes)
        if(days_between != 0 && t2.compareTo(t1) == -1) {
            return (int) (t1.until(t2, ChronoUnit.MINUTES) + days_between * 24 * 60);
        }
        if(days_between != 0 && t2.compareTo(t1) == 1) {
            return (int) (-t1.until(t2, ChronoUnit.MINUTES) + days_between * 24 * 60);
        }
        layover_time = (int)(Math.abs(t1.until(t2, ChronoUnit.MINUTES)) + days_between * 24 * 60);
        return layover_time;
    }

    //get the layover time, in minutes, between two flights (For travel command)
    public static int layover(Flight x, Flight y) {
        int layover_time = 0;
        // Get the days between two flights
        int days_between = Days_between(Flight.arrival_day_week(x), y.getDep_dayofWeek());
        // Calculate the amount of layover time (in minutes)
        if(days_between != 0 && y.getDep_local_time().compareTo(Flight.arrival_l_time(x)) < 0) {
            return (int) (-Flight.arrival_l_time(x).until(y.getDep_local_time(), ChronoUnit.MINUTES) + days_between * 24 * 60);
        }
        if(days_between != 0 && y.getDep_local_time().compareTo(Flight.arrival_l_time(x)) > 0) {
            return (int) (Flight.arrival_l_time(x).until(y.getDep_local_time(), ChronoUnit.MINUTES) + days_between * 24 * 60);
        }
        layover_time = (int)(Math.abs(Flight.arrival_l_time(x).until(y.getDep_local_time(), ChronoUnit.MINUTES)) + days_between * 24 * 60);
        return layover_time;
    }
    // toString method for export command
    public String flight_toString() {
        DateTimeFormatter time_Formatter = DateTimeFormatter.ofPattern("H:mm")
                .withResolverStyle(ResolverStyle.STRICT);

        return dep_dayofWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()) + " " +
                LocalTime.parse(dep_local_time.toString(), time_Formatter).toString()+ ","
                + dep_location.getLocation_name() + "," + arr_location.getLocation_name()
                + "," + capacity + "," + num_of_booked + "\n";
    }

    // The following are the getters & setters for private attributes :)

    // Get the flight ID
    public int getFlight_id() {

        return flight_id;
    }

    // Get the departure day of week
    public DayOfWeek getDep_dayofWeek() {

        return dep_dayofWeek;
    }

    // Get the departure loacal time
    public LocalTime getDep_local_time() {

        return dep_local_time;
    }

    // Get the destination
    public Location getArr_location() {

        return arr_location;
    }

    // Get the source
    public Location getDep_location() {

        return dep_location;
    }

    // Get the capacity
    public int getCapacity() {

        return capacity;
    }

    // Get the number of passengers booked the flight
    public void setNum_of_booked(int num_of_booked) {

        this.num_of_booked = num_of_booked;
    }

    public int getNum_of_booked() {

        return num_of_booked;
    }

}
