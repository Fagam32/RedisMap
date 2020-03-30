import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;


public class RedisMapTest {
    @Test
    public void baseTests() {
        Map<String, String> map1 = new RedisMap();
        Map<String, String> map2 = new RedisMap();

        map1.put("one", "1");

        map2.put("one", "ONE");
        map2.put("two", "TWO");

        assertEquals("1", map1.get("one"));
        assertEquals(1, map1.size());
        assertEquals(2, map2.size());

        map1.put("one", "first");

        assertEquals("first", map1.get("one"));
        assertEquals(1, map1.size());

        assertTrue(map1.containsKey("one"));
        assertFalse(map1.containsKey("two"));

        Set<String> keys2 = map2.keySet();
        assertEquals(2, keys2.size());
        assertTrue(keys2.contains("one"));
        assertTrue(keys2.contains("two"));

        Collection<String> values1 = map1.values();
        assertEquals(1, values1.size());
        assertTrue(values1.contains("first"));
        System.gc();
    }

    @Test
    public void size() {
        try (RedisMap redisMap = new RedisMap()) {
            assertEquals(redisMap.size(), 0);
            redisMap.put("1", "value1");
            redisMap.put("2", "value2");
            redisMap.put("3", "value3");
            assertEquals(redisMap.size(), 3);
        }
    }

    @Test
    public void delete() {
        try (RedisMap redisMap = new RedisMap()) {
            redisMap.put("1", "value1");
            redisMap.put("2", "value2");
            redisMap.put("3", "value3");

            redisMap.remove("4");

            assertEquals(redisMap.size(), 3);

            redisMap.remove("3");

            assertEquals(redisMap.size(), 2);
        }
    }

    @Test
    public void add() {
        try (RedisMap redisMap = new RedisMap()) {
            for (int i = 0; i < 1000; i++)
                redisMap.put("" + i, "value " + i);
            for (int i = 0; i < 1000; i++) {
                assertEquals(redisMap.get("" + i), "value " + i);
            }
        }
    }

    @Test
    public void twoMapsDifferentHashes() {
        try (RedisMap redisMap1 = new RedisMap();
             RedisMap redisMap2 = new RedisMap()) {
            redisMap1.put("1", "value1");
            redisMap1.put("2", "value2");
            redisMap1.put("3", "value3");

            redisMap2.put("1", "value21");
            redisMap2.put("2", "value22");
            redisMap2.put("3", "value23");

            assertEquals(redisMap1.size(), 3);
            assertEquals(redisMap2.size(), 3);

            for(int i = 1; i <= 3; i++){
                assertEquals(redisMap1.get("" + i), "value" + i);
                assertEquals(redisMap2.get("" + i), "value2" + i);
            }
        }
    }
    @Test
    public void twoMapsSameHashes(){
        try(RedisMap redisMap1 = new RedisMap();
            RedisMap redisMap2 = new RedisMap()){

            redisMap1.put("1", "value1");
            redisMap1.put("2", "value2");
            redisMap1.put("3", "value3");

            redisMap2.setHash(redisMap1.getHash());

            assertEquals(redisMap2.size(), 3);

            assertEquals(redisMap2.get("1"), "value1");
            assertEquals(redisMap2.get("2"), "value2");
            assertEquals(redisMap2.get("3"), "value3");

            redisMap2.remove("2");

            assertEquals(redisMap2.size(), 2);
            assertFalse(redisMap2.containsKey("2"));
        }
    }
}