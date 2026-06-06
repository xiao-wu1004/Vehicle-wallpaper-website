package com.vehiclewallpaper.backend.user;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REGISTRATIONS = 3;
    private static final int WINDOW_MINUTES = 15;

    private final ConcurrentHashMap<String, RateWindow> loginWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateWindow> registerWindows = new ConcurrentHashMap<>();

    public boolean allowLogin(String ip) {
        return allow(loginWindows, ip, MAX_LOGIN_ATTEMPTS, WINDOW_MINUTES);
    }

    public boolean allowRegistration(String ip) {
        return allow(registerWindows, ip, MAX_REGISTRATIONS, WINDOW_MINUTES);
    }

    public int remainingLoginAttempts(String ip) {
        return remaining(loginWindows, ip, MAX_LOGIN_ATTEMPTS, WINDOW_MINUTES);
    }

    public int remainingRegistrations(String ip) {
        return remaining(registerWindows, ip, MAX_REGISTRATIONS, WINDOW_MINUTES);
    }

    private boolean allow(ConcurrentHashMap<String, RateWindow> windows, String key, int max, int minutes) {
        windows.values().removeIf(w -> w.isExpired(minutes));
        RateWindow window = windows.computeIfAbsent(key, k -> new RateWindow());
        window.prune(minutes);
        if (window.count() >= max) {
            return false;
        }
        window.record();
        return true;
    }

    private int remaining(ConcurrentHashMap<String, RateWindow> windows, String key, int max, int minutes) {
        RateWindow window = windows.get(key);
        if (window == null) {
            return max;
        }
        window.prune(minutes);
        return Math.max(0, max - window.count());
    }

    private static class RateWindow {
        private final ConcurrentHashMap<Long, Integer> slots = new ConcurrentHashMap<>();

        void record() {
            long slot = slotKey();
            slots.merge(slot, 1, Integer::sum);
        }

        int count() {
            return slots.values().stream().mapToInt(Integer::intValue).sum();
        }

        void prune(int windowMinutes) {
            long cutoff = slotKey() - windowMinutes * 60_000L;
            slots.keySet().removeIf(s -> s < cutoff);
        }

        boolean isExpired(int windowMinutes) {
            prune(windowMinutes);
            return slots.isEmpty();
        }

        private long slotKey() {
            return System.currentTimeMillis() / 1000L;
        }
    }
}
