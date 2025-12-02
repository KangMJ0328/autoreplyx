package com.autoreplyx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Map;

/**
 * Development configuration that provides an in-memory mock Redis implementation.
 * This allows running the application without a real Redis server.
 */
@Configuration
@Profile("dev")
public class DevRedisConfig {

    private final Map<String, String> stringStore = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<String>> listStore = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return new MockRedisTemplate(stringStore, listStore);
    }

    /**
     * Mock RedisTemplate that stores data in memory
     */
    public static class MockRedisTemplate extends RedisTemplate<String, String> {
        private final Map<String, String> stringStore;
        private final Map<String, ConcurrentLinkedDeque<String>> listStore;
        private final MockValueOperations valueOps;
        private final MockListOperations listOps;

        public MockRedisTemplate(Map<String, String> stringStore,
                                  Map<String, ConcurrentLinkedDeque<String>> listStore) {
            this.stringStore = stringStore;
            this.listStore = listStore;
            this.valueOps = new MockValueOperations(stringStore);
            this.listOps = new MockListOperations(listStore);
        }

        @Override
        public void afterPropertiesSet() {
            // Skip the parent's afterPropertiesSet which requires a connection factory
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOps;
        }

        @Override
        public ListOperations<String, String> opsForList() {
            return listOps;
        }

        @Override
        public Boolean delete(String key) {
            stringStore.remove(key);
            listStore.remove(key);
            return true;
        }

        @Override
        public Boolean hasKey(String key) {
            return stringStore.containsKey(key) || listStore.containsKey(key);
        }
    }

    /**
     * Mock ValueOperations
     */
    public static class MockValueOperations implements ValueOperations<String, String> {
        private final Map<String, String> store;

        public MockValueOperations(Map<String, String> store) {
            this.store = store;
        }

        @Override
        public void set(String key, String value) {
            store.put(key, value);
        }

        @Override
        public void set(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            store.put(key, value);
        }

        @Override
        public Boolean setIfAbsent(String key, String value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, Duration timeout) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Boolean setIfPresent(String key, String value) {
            if (store.containsKey(key)) {
                store.put(key, value);
                return true;
            }
            return false;
        }

        @Override
        public Boolean setIfPresent(String key, String value, long timeout, java.util.concurrent.TimeUnit unit) {
            return setIfPresent(key, value);
        }

        @Override
        public Boolean setIfPresent(String key, String value, Duration timeout) {
            return setIfPresent(key, value);
        }

        @Override
        public String get(Object key) {
            return store.get(key.toString());
        }

        @Override
        public String getAndDelete(String key) {
            return store.remove(key);
        }

        @Override
        public String getAndExpire(String key, long timeout, java.util.concurrent.TimeUnit unit) {
            return store.get(key);
        }

        @Override
        public String getAndExpire(String key, Duration timeout) {
            return store.get(key);
        }

        @Override
        public String getAndPersist(String key) {
            return store.get(key);
        }

        @Override
        public String getAndSet(String key, String value) {
            return store.put(key, value);
        }

        @Override
        public Long increment(String key) {
            String val = store.getOrDefault(key, "0");
            long newVal = Long.parseLong(val) + 1;
            store.put(key, String.valueOf(newVal));
            return newVal;
        }

        @Override
        public Long increment(String key, long delta) {
            String val = store.getOrDefault(key, "0");
            long newVal = Long.parseLong(val) + delta;
            store.put(key, String.valueOf(newVal));
            return newVal;
        }

        @Override
        public Double increment(String key, double delta) {
            String val = store.getOrDefault(key, "0");
            double newVal = Double.parseDouble(val) + delta;
            store.put(key, String.valueOf(newVal));
            return newVal;
        }

        @Override
        public Long decrement(String key) {
            return increment(key, -1);
        }

        @Override
        public Long decrement(String key, long delta) {
            return increment(key, -delta);
        }

        @Override
        public Integer append(String key, String value) {
            String current = store.getOrDefault(key, "");
            store.put(key, current + value);
            return store.get(key).length();
        }

        @Override
        public String get(String key, long start, long end) {
            String val = store.get(key);
            if (val == null) return null;
            return val.substring((int) start, Math.min((int) end + 1, val.length()));
        }

        @Override
        public void set(String key, String value, long offset) {
            store.put(key, value);
        }

        @Override
        public Long size(String key) {
            String val = store.get(key);
            return val == null ? 0L : (long) val.length();
        }

        @Override
        public Boolean setBit(String key, long offset, boolean value) {
            return false;
        }

        @Override
        public Boolean getBit(String key, long offset) {
            return false;
        }

        @Override
        public java.util.List<Long> bitField(String key, org.springframework.data.redis.connection.BitFieldSubCommands subCommands) {
            return java.util.Collections.emptyList();
        }

        @Override
        public RedisOperations<String, String> getOperations() {
            return null;
        }

        @Override
        public void multiSet(Map<? extends String, ? extends String> map) {
            store.putAll(map);
        }

        @Override
        public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
            for (String key : map.keySet()) {
                if (store.containsKey(key)) return false;
            }
            store.putAll(map);
            return true;
        }

        @Override
        public java.util.List<String> multiGet(java.util.Collection<String> keys) {
            return keys.stream().map(store::get).collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Mock ListOperations
     */
    public static class MockListOperations implements ListOperations<String, String> {
        private final Map<String, ConcurrentLinkedDeque<String>> store;

        public MockListOperations(Map<String, ConcurrentLinkedDeque<String>> store) {
            this.store = store;
        }

        private ConcurrentLinkedDeque<String> getList(String key) {
            return store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        }

        @Override
        public Long leftPush(String key, String value) {
            getList(key).addFirst(value);
            return (long) getList(key).size();
        }

        @Override
        public Long leftPushAll(String key, String... values) {
            for (String v : values) {
                getList(key).addFirst(v);
            }
            return (long) getList(key).size();
        }

        @Override
        public Long leftPushAll(String key, java.util.Collection<String> values) {
            for (String v : values) {
                getList(key).addFirst(v);
            }
            return (long) getList(key).size();
        }

        @Override
        public Long leftPushIfPresent(String key, String value) {
            if (store.containsKey(key)) {
                return leftPush(key, value);
            }
            return 0L;
        }

        @Override
        public Long leftPush(String key, String pivot, String value) {
            return leftPush(key, value);
        }

        @Override
        public Long rightPush(String key, String value) {
            getList(key).addLast(value);
            return (long) getList(key).size();
        }

        @Override
        public Long rightPushAll(String key, String... values) {
            for (String v : values) {
                getList(key).addLast(v);
            }
            return (long) getList(key).size();
        }

        @Override
        public Long rightPushAll(String key, java.util.Collection<String> values) {
            for (String v : values) {
                getList(key).addLast(v);
            }
            return (long) getList(key).size();
        }

        @Override
        public Long rightPushIfPresent(String key, String value) {
            if (store.containsKey(key)) {
                return rightPush(key, value);
            }
            return 0L;
        }

        @Override
        public Long rightPush(String key, String pivot, String value) {
            return rightPush(key, value);
        }

        @Override
        public String leftPop(String key) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            return list == null ? null : list.pollFirst();
        }

        @Override
        public java.util.List<String> leftPop(String key, long count) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                String val = leftPop(key);
                if (val == null) break;
                result.add(val);
            }
            return result;
        }

        @Override
        public String leftPop(String key, long timeout, java.util.concurrent.TimeUnit unit) {
            return leftPop(key);
        }

        @Override
        public String leftPop(String key, Duration timeout) {
            return leftPop(key);
        }

        @Override
        public String rightPop(String key) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            return list == null ? null : list.pollLast();
        }

        @Override
        public java.util.List<String> rightPop(String key, long count) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                String val = rightPop(key);
                if (val == null) break;
                result.add(val);
            }
            return result;
        }

        @Override
        public String rightPop(String key, long timeout, java.util.concurrent.TimeUnit unit) {
            return rightPop(key);
        }

        @Override
        public String rightPop(String key, Duration timeout) {
            return rightPop(key);
        }

        @Override
        public String rightPopAndLeftPush(String sourceKey, String destinationKey) {
            String val = rightPop(sourceKey);
            if (val != null) {
                leftPush(destinationKey, val);
            }
            return val;
        }

        @Override
        public String rightPopAndLeftPush(String sourceKey, String destinationKey, long timeout, java.util.concurrent.TimeUnit unit) {
            return rightPopAndLeftPush(sourceKey, destinationKey);
        }

        @Override
        public String rightPopAndLeftPush(String sourceKey, String destinationKey, Duration timeout) {
            return rightPopAndLeftPush(sourceKey, destinationKey);
        }

        @Override
        public String move(String sourceKey, org.springframework.data.redis.connection.RedisListCommands.Direction from, String destKey, org.springframework.data.redis.connection.RedisListCommands.Direction to) {
            return null;
        }

        @Override
        public String move(String sourceKey, org.springframework.data.redis.connection.RedisListCommands.Direction from, String destKey, org.springframework.data.redis.connection.RedisListCommands.Direction to, long timeout, java.util.concurrent.TimeUnit unit) {
            return null;
        }

        @Override
        public String move(String sourceKey, org.springframework.data.redis.connection.RedisListCommands.Direction from, String destKey, org.springframework.data.redis.connection.RedisListCommands.Direction to, Duration timeout) {
            return null;
        }

        @Override
        public void set(String key, long index, String value) {
            // Not fully implemented
        }

        @Override
        public Long remove(String key, long count, Object value) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            if (list == null) return 0L;
            long removed = 0;
            while (list.remove(value.toString())) {
                removed++;
                if (count > 0 && removed >= count) break;
            }
            return removed;
        }

        @Override
        public String index(String key, long index) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            if (list == null) return null;
            int i = 0;
            for (String val : list) {
                if (i++ == index) return val;
            }
            return null;
        }

        @Override
        public Long indexOf(String key, String value) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            if (list == null) return null;
            long i = 0;
            for (String val : list) {
                if (val.equals(value)) return i;
                i++;
            }
            return null;
        }

        @Override
        public Long lastIndexOf(String key, String value) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            if (list == null) return null;
            long lastIdx = -1;
            long i = 0;
            for (String val : list) {
                if (val.equals(value)) lastIdx = i;
                i++;
            }
            return lastIdx >= 0 ? lastIdx : null;
        }

        @Override
        public java.util.List<String> range(String key, long start, long end) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            if (list == null) return java.util.Collections.emptyList();
            java.util.List<String> result = new java.util.ArrayList<>(list);
            int s = (int) start;
            int e = (int) Math.min(end + 1, result.size());
            if (s >= result.size()) return java.util.Collections.emptyList();
            return result.subList(s, e);
        }

        @Override
        public void trim(String key, long start, long end) {
            // Not fully implemented
        }

        @Override
        public Long size(String key) {
            ConcurrentLinkedDeque<String> list = store.get(key);
            return list == null ? 0L : (long) list.size();
        }

        @Override
        public RedisOperations<String, String> getOperations() {
            return null;
        }
    }
}
