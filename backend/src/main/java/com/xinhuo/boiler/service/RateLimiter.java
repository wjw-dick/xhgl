package com.xinhuo.boiler.service;

import com.xinhuo.boiler.common.BizException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiter {
    private final Map<String, ConcurrentLinkedDeque<Long>> windows = new ConcurrentHashMap<>();
    private final Map<String, Long> onceKeys = new ConcurrentHashMap<>();

    public void check(String key, int limit, int seconds) {
        long now = System.currentTimeMillis();
        long from = now - seconds * 1000L;
        ConcurrentLinkedDeque<Long> deque = windows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            while (deque.peekFirst() != null && deque.peekFirst() < from) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new BizException(429, "操作过于频繁，请稍后再试");
            }
            deque.addLast(now);
        }
        if (windows.size() > 20000) {
            windows.clear();
        }
    }

    public void once(String key, int ttlSeconds) {
        long now = System.currentTimeMillis();
        onceKeys.entrySet().removeIf(e -> e.getValue() < now);
        Long previous = onceKeys.putIfAbsent(key, now + ttlSeconds * 1000L);
        if (previous != null) {
            throw new BizException(409, "请勿重复提交");
        }
    }
}
