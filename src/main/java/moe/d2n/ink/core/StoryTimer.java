package moe.d2n.ink.core;

import net.mamoe.mirai.BotFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StoryTimer {
    public int day = getNowDay();
    public static final StoryTimer INSTANCE = StoryTimerInstance.INSTANCE;

    static class StoryTimerInstance {
        private static final StoryTimer INSTANCE = new StoryTimer();
    }

    public static int getNowDay() {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant now = Instant.now();
        ZonedDateTime localDateTime = now.atZone(zoneId);
        ZonedDateTime epochStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zoneId);
        return (int) ChronoUnit.DAYS.between(epochStart, localDateTime);
    }

    protected StoryTimer() {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime nextMidnight = now.toLocalDate().atStartOfDay().plusDays(1);

        long initialDelay = Duration.between(now, nextMidnight).toMillis();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> day = getNowDay(), initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }
}
