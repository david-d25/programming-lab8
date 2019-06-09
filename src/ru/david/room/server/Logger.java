package ru.david.room.server;

import ru.david.room.Utils;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static ru.david.room.GlobalConstants.*;

class Logger {
    private PrintStream out;
    private PrintStream err;

    Logger(PrintStream logsOutput, PrintStream errorsOutput) {
        this.out = logsOutput;
        this.err = errorsOutput;
    }

    void log(String message) {
        out.println(generateLogTime() + message);
        out.flush();
    }

    void err(String message) {
        err.println(Utils.colorize("[[red]]" + generateLogTime() + "[ ERROR ] " + message + "[[reset]]"));
        err.flush();
    }

    void warn(String message) {
        err.println(Utils.colorize("[[yellow]]" + generateLogTime() + "[ WARNING ] " + message + "[[reset]]"));
        err.flush();
    }

    private String generateLogTime() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        return String.format("[%02d.%02d.%s %02d:%02d:%02d] ",
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.MONTH)+1,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
    }
}
