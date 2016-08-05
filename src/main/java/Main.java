/**
 * Created by alison on 8/3/16.
 */

import static spark.Spark.*;
import com.flatironschool.javacs.*;

import redis.clients.jedis.Jedis;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static void crawl(WikiCrawler wc) throws IOException {
        // loop until we index a new page
        String res;
        do {
            res = wc.crawl(false);
        } while (res == null);
    }

    public static void main(String[] args) throws IOException {
        port(9999);

        // make a JedisIndex
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);

        // set up crawler
        String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
        WikiCrawler wc = new WikiCrawler(source, index);
        wc.loadQueue(source);

        get("/search/:term", (req, res) -> {
            //crawl(wc);
            WikiSearch search = WikiSearch.search(req.params(":term"),index);
            List<Map.Entry<String, Integer>> entries = search.sort();
            Map map = new HashMap();
            map.put("term", req.params(":term"));
            map.put("entries", entries);
            return new ModelAndView(map, "results.hbs");
        }, new HandlebarsTemplateEngine());

        get("/", (req, res) -> {
            //crawl(wc);
            Map map = new HashMap();
            return new ModelAndView(map, "search.hbs");
        }, new HandlebarsTemplateEngine());

        post("/search", (req, res) -> {
            //crawl(wc);
            WikiSearch search = WikiSearch.search(req.queryParams("term"),index);
            List<Map.Entry<String, Integer>> entries = search.sort();
            Map map = new HashMap();
            map.put("term", req.queryParams("term"));
            map.put("entries", entries);
            return new ModelAndView(map, "results.hbs");
        }, new HandlebarsTemplateEngine());
    }
}
