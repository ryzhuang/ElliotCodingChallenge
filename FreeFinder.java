import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class FreeFinder {
	
	private static String FILE_NOT_FOUND_MESSAGE = "File not found in path.";
	private static String FILE_ACCESS_ERROR_MESSAGE = "There was a problem accessing the file.";
	private static String DATE_TIME_FORMATTING_ERROR_MESSAGE = "There was a problem reading the date.";
	private static String LONGEST_FREE_PERIOD_MESSAGE = "Here are the longest free periods over the week starting TOMORROW:";
	
	private HashMap<Integer, ArrayList<DatePair>> mFreeListByDay;
	private ArrayList<DatePair> mBusyList;
	
	private LocalDateTime availMeetStart;
	private LocalDateTime availMeetEnd;
	
	private DateTimeFormatter mDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private DateTimeFormatter mTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	/**
	 * Calculates the largest duration of free time given the input file
	 * The durations are grouped in days leading up to a week the program is run
	 *
	 * @param	The String filepath for the location of the input CSV file
	 * @return	A String block showing the largest free period over 7 days
	 */
	public String returnLargestFreeTime(String filePath) {
		
		//Initialization of Lists/Maps
		mFreeListByDay = new HashMap<>();
		mFreeListByDay.put(0, new ArrayList<DatePair>());
		mFreeListByDay.put(1, new ArrayList<DatePair>());
		mFreeListByDay.put(2, new ArrayList<DatePair>());
		mFreeListByDay.put(3, new ArrayList<DatePair>());
		mFreeListByDay.put(4, new ArrayList<DatePair>());
		mFreeListByDay.put(5, new ArrayList<DatePair>());
		mFreeListByDay.put(6, new ArrayList<DatePair>());
		
		mBusyList = new ArrayList<DatePair>();
		
		//Current Date calculated at beginning of run
		LocalDateTime currentDateTime = LocalDateTime.now();
		
		getUnavailableDays(currentDateTime);
		parseCalendarEvents(filePath);
		
		//Sort the busy periods chronologically
		Collections.sort(mBusyList, new Comparator<DatePair>() {
		    @Override
		    public int compare(DatePair first, DatePair second) {
		    	return first.startDate.compareTo(second.startDate);
		    }
		});
		
		//Iterate through busy periods. A gap in the busy period is added as a free period in the respective
		//list based on the periods day of the week. Use days of week to prevent confusion with days of month/year
		LocalDateTime currentEndDateTime = availMeetStart;
		for(DatePair currentBusy : mBusyList) {
			//Inclusive before
			if (currentBusy.startDate.isAfter(currentEndDateTime)) {
				switch(currentEndDateTime.getDayOfWeek()) {
				case MONDAY:
					mFreeListByDay.get(0).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case TUESDAY:
					mFreeListByDay.get(1).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case WEDNESDAY:
					mFreeListByDay.get(2).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case THURSDAY:
					mFreeListByDay.get(3).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case FRIDAY:
					mFreeListByDay.get(4).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case SATURDAY:
					mFreeListByDay.get(5).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				case SUNDAY:
				default:
					mFreeListByDay.get(6).add(new DatePair(currentEndDateTime, currentBusy.startDate));
					break;
				}
			}		
			currentEndDateTime = currentBusy.endDate;	
		}
		
		//Find largest free period for each day and record into a string message
		StringBuilder sb = new StringBuilder(LONGEST_FREE_PERIOD_MESSAGE);
		sb.append("\n");
		LocalDateTime currentDay;
		DatePair longestPeriod = null;
		for (int i = 1; i < 8; i++) {
			currentDay = currentDateTime.plusDays(i);
			switch(currentDay.getDayOfWeek()) {
			case MONDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(0));
				break;
			case TUESDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(1));
				break;
			case WEDNESDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(2));
				break;
			case THURSDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(3));
				break;
			case FRIDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(4));
				break;
			case SATURDAY:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(5));
				break;
			case SUNDAY:
			default:
				longestPeriod = getLongestPeriod(mFreeListByDay.get(6));
				break;
			}
			
			sb.append("On ");
			sb.append(currentDay.format(mDateFormatter));
			sb.append(", the largest available free period is between: ");
			sb.append(longestPeriod.startDate.format(mTimeFormatter));
			sb.append(", ");
			sb.append(longestPeriod.endDate.format(mTimeFormatter));
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * Calculates longest period of time by comparing the Duration between start and end dates.
	 *
	 * @param	The list of DatePair objects.
	 * @return	DatePair representing the largest duration.
	 */
	private DatePair getLongestPeriod(ArrayList<DatePair> periodList) {
		DatePair longestPeriod = Collections.max(periodList, new Comparator<DatePair>() {
		    @Override
		    public int compare(DatePair first, DatePair second) {
		    	Duration firstDuration = Duration.between(first.startDate, first.endDate);
		    	Duration secondDuration = Duration.between(second.startDate, second.endDate);
		        if (firstDuration.getSeconds() > secondDuration.getSeconds())
		            return 1;
		        else if (firstDuration.getSeconds() < secondDuration.getSeconds())
		            return -1;
		        return 0;
		    }
		});
		return longestPeriod;
	}
	
	/**
	 * Calculates periods of time that are off-work hours and are unavailable for meetings.
	 * Unavailable times start at the next day from 12AM to 8AM, and 10PM to 8AM for days
	 * after - up to a week starting the next day.
	 *
	 * @param	The LocalDateTime representing the time in which the program is run
	 * @return	N/A
	 */
	private void getUnavailableDays(LocalDateTime currentDateTime) {
		LocalDateTime currentDate8AM;
		LocalDateTime currentDate10PM;
		
		currentDate8AM = LocalDateTime.of(currentDateTime.getYear(), currentDateTime.getMonth(), currentDateTime.getDayOfMonth(), 8, 0);
		currentDate10PM = LocalDateTime.of(currentDateTime.getYear(), currentDateTime.getMonth(), currentDateTime.getDayOfMonth(), 22, 0);
		
		availMeetStart = currentDate8AM.plusDays(1);
		availMeetEnd = currentDate10PM.plusDays(7);
		
		mBusyList.add(new DatePair(currentDate10PM.plusDays(1), currentDate8AM.plusDays(2)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(2), currentDate8AM.plusDays(3)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(3), currentDate8AM.plusDays(4)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(4), currentDate8AM.plusDays(5)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(5), currentDate8AM.plusDays(6)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(6), currentDate8AM.plusDays(7)));
		mBusyList.add(new DatePair(currentDate10PM.plusDays(7), currentDate8AM.plusDays(8)));
	}

	/**
	 * Reads the contents of the input file, parses the text into LocalDateTime Objects.
	 * Stores busy periods that fall within the 7 week time frame
	 *
	 * @param	The filePath in which the input file is located in directory
	 * @return	N/A
	 */
	private void parseCalendarEvents(String filePath) {
		BufferedReader br = null;
		String readLine;
		String[] splitLine = new  String[3];
		final String splitLineBy = ",";
		
		LocalDateTime readStartDate, readEndDate;
		
		try {
			br = new BufferedReader(new FileReader(filePath));
			while ((readLine = br.readLine()) != null) {
				
				//Use split character separator
				splitLine = readLine.split(splitLineBy);
				readStartDate = LocalDateTime.parse(splitLine[1].trim(), mDateTimeFormatter);
				readEndDate = LocalDateTime.parse(splitLine[2].trim(), mDateTimeFormatter);
				
				//Check if busy period overlaps with the available meeting dates
				if (availMeetStart.isBefore(readEndDate) && readStartDate.isBefore(availMeetEnd)) 
					mBusyList.add(new DatePair(readStartDate, readEndDate));		
			}
		} catch (FileNotFoundException e) {
			System.out.print(FILE_NOT_FOUND_MESSAGE);
            e.printStackTrace();
        } catch (IOException e) {
        	System.out.print(FILE_ACCESS_ERROR_MESSAGE);
            e.printStackTrace();
        } catch (DateTimeParseException e) {
        	System.out.print(DATE_TIME_FORMATTING_ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                	//Error in freeing br is not user facing. No message needed
                    e.printStackTrace();
                }
            }
        }	
	}
	
	/**
	 * Custom class representing a DatePair.
	 * Consists of a startDate and endDate representing a period of time.
	 */
	class DatePair{
		final LocalDateTime startDate;
		final LocalDateTime endDate;

		public DatePair(LocalDateTime startDate, LocalDateTime endDate) {
			this.startDate = startDate;
			this.endDate = endDate;
		}	
	}
}
