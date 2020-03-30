import redis.clients.jedis.Jedis;

import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Saves all key:values in Redis directly
 * All keys are saved as key.concat(hash) for usage in different instances and different apps
 * Hash generates as random String length of 10 , so the probability of collision is very(very) small
 * As a result same hash => same Map
 * It implements AutoCloseable(instead of deprecated finalize()), and the best usage for it in
 * try-with-resources OR you should close it by yourself OR call System.gc() . If you don't do it, the map will
 * be saved in Redis as it is. You can use isSaved parameter as well to make sure your map will be saved.
 */
public class RedisMap implements Map<String, String>, AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final Cleaner.Cleanable cleanable;

    private String host;
    private int port;
    private String hash;
    private boolean isSaved = false;

    public RedisMap() {
        this("localhost", 6379, generateNewHash());
    }

    public RedisMap(String hash) {
        this("localhost", 6379, hash);
    }

    public RedisMap(String host, int port) {
        this(host, port, generateNewHash());
    }

    static private String generateNewHash() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            + "abcdefhijklmnopqrstuvwxyz"
                            + "1234566789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index =(int) (Math.random() * alphabet.length());
            sb.append(alphabet.charAt(index));
        }
        return sb.toString();
    }

    public RedisMap(String host, int port, String hash) {

        if (host == null || hash == null) throw new NullPointerException();

        this.host = host;
        this.port = port;
        this.hash = hash;

        cleanable = CLEANER.register(this, this::clear);
    }

    @Override
    public int size() {
        Jedis jedis = new Jedis(host, port);
        return jedis.keys("*" + hash).size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        Jedis jedis = new Jedis(host, port);
        return jedis.get(key + hash) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) throw new NullPointerException();

        Jedis jedis = new Jedis(host, port);
        Set<String> keys = jedis.keys("*" + hash);
        for (String x : keys) {
            keys.add(x.replace(hash, ""));
        }
        return keys.stream().map(jedis::get).anyMatch(s -> s.equals(value));
    }

    @Override
    public String replace(String key, String value) {
        if (key == null || value == null) throw new NullPointerException();
        return put(key, value);
    }


    @Override
    public String get(Object key) {
        if (key == null) throw new NullPointerException();

        Jedis jedis = new Jedis(host, port);
        return jedis.get(key + hash);
    }

    @Override
    public String put(String key, String value) {
        if (key == null || value == null) throw new NullPointerException();

        Jedis jedis = new Jedis(host, port);
        return jedis.set(key + hash, value);
    }

    @Override
    public String remove(Object key) {
        if (key == null) throw new NullPointerException();

        Jedis jedis = new Jedis(host, port);
        String prev = jedis.get(key + hash);
        jedis.del(key + hash);
        return prev;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> entry) {
        Jedis jedis = new Jedis(host, port);
        for (Map.Entry<? extends String, ? extends String> pair : entry.entrySet())
            jedis.set(pair.getKey() + hash, pair.getValue());
    }

    @Override
    public void clear() {
        Jedis jedis = new Jedis(host, port);
        Set<String> keys = jedis.keys("*" + hash);
        for (String key : keys)
            jedis.del(key);
    }

    @Override
    public Set<String> keySet() {
        Jedis jedis = new Jedis(host, port);
        return jedis.keys("*" + hash).stream()
                .map(x -> x.replace(hash, ""))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<String> values() {
        Jedis jedis = new Jedis(host, port);
        Set<String> keys = jedis.keys("*" + hash);
        Collection<String> values = new ArrayList<>();
        for (String key : keys)
            values.add(jedis.get(key));
        return values;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Jedis jedis = new Jedis(host, port);
        Map<String, String> map = new HashMap<>();
        Set<String> keys = jedis.keys("*" + hash);

        for (String key : keys) {
            map.put(key.replace(hash, ""), jedis.get(key));
        }
        return map.entrySet();
    }

    @Override
    public void close() {
        if (!isSaved) {
            cleanable.clean();
        }
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        if (hash == null) throw new NullPointerException();

        this.hash = hash;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void save() {
        this.isSaved = true;
    }

    public void dontSave() {
        this.isSaved = false;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
