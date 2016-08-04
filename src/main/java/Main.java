/**
 * Created by alison on 8/3/16.
 */

import static spark.Spark.*;
import com.flatironschool.javacs.*;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        port(9999);

        // make a JedisIndex
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);

        get("/hello", (req, res) -> "Hello World");

        // matches "GET /hello/foo" and "GET /hello/bar"
        // request.params(":name") is 'foo' or 'bar'
        get("/hello/:name", (request, response) -> {
            return "Hello: " + request.params(":name");
        });

        get("/search/:term", (req, res) -> {
            WikiSearch search = WikiSearch.search(req.params(":term"),index);
            List<Map.Entry<String, Integer>> entries = search.sort();
            return entries;
        });
    }
}
