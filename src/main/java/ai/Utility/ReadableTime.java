package ai.Utility;

public class ReadableTime {
    public String compute(long seconds) {
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;

        String result = "";
        if (days > 0) {
            if (days == 1 && sec == 0 && minutes == 0 && hours == 0) {
                result += " " + days + " Day";
            } else if (days == 1) {
                result += " " + days + " Day";
            } else result += " " + days + " Days";
        }
        if (hours > 0) {
            if (hours == 1) {
                result += " " + hours + " Hour";
            } else result += " " + hours + " Hours";
        }
        if (minutes > 0) {
            if (minutes == 1) {
                result += " " + minutes + " Minute";
            } else result += " " + minutes + " Minutes";
        }
        if (sec > 0) {
            if (sec == 1) {
                result += " " + sec + " Second";
            } else result += " " + sec + " Seconds";
        }
        String[] split = result.split(" ");
        if (split.length > 3) {
            result = "";
            for (int i = 0; i < split.length-2; i++) {
                result += split[i] + " ";
            }
            result += "and " + split[split.length-2] + " " + split[split.length-1];
        }
        
        return result;
    }
}
