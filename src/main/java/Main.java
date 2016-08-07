/**
 * Created by alison on 8/3/16.
 */

import static spark.Spark.*;

import redis.clients.jedis.Jedis;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.io.IOException;
import java.util.*;

public class Main {
    private static void crawl(WikiCrawler wc) throws IOException {
        // loop until we index a new page
        String res;
        do {
            res = wc.crawl(false);
        } while (res == null);
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    public static void main(String[] args) throws IOException {
        port(getHerokuAssignedPort());
        staticFileLocation("/public");

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

            // add titles rather than links to show in results
            List<Map.Entry<String, Map.Entry<String, Integer>>> titles = new LinkedList<>();
            for (Map.Entry<String, Integer> entry: entries) {
                String title = entry.getKey().replaceAll("https://en.wikipedia.org/wiki/", "");
                title = title.replaceAll("_", " ");
                titles.add(new AbstractMap.SimpleEntry<>(title, entry));
            }
            Map map = new HashMap();
            map.put("term", req.params(":term"));
            map.put("titles", titles);
            map.put("listsize", entries.size());
            return new ModelAndView(map, "results.hbs");
        }, new HandlebarsTemplateEngine());

        get("/", (req, res) -> {
            //crawl(wc);
            Map map = new HashMap();
            return new ModelAndView(map, "search.hbs");
        }, new HandlebarsTemplateEngine());

        post("/search", (req, res) -> {
            res.redirect("/search/"+req.queryParams("term"));
            return null;
        });
    }
}
