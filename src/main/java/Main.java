public class Main {

    public static void main(String[] args) {
        try(RedisMap redisMap = new RedisMap()) {
            redisMap.put("1", "value");
            redisMap.put("2", "value");
            redisMap.put("3", "value");
        }
    }
}
