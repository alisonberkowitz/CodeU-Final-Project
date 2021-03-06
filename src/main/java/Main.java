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
        String source = "https://en.wikipedia.org/wiki/Lilac_(color)";
        WikiCrawler wc = new WikiCrawler(source, index);
        wc.loadQueue(source);

        get("/search/:term", (req, res) -> {
//            crawl(wc);
            String[] terms = req.params(":term").split("\\P{Alpha}+");
            int nextTerm = 1;
            WikiSearch andsearch;
            WikiSearch orsearch;
            if ((TermCounter.stopwords.contains(terms[0].toLowerCase())) && terms.length > 1) {
                andsearch = WikiSearch.search(terms[1].toLowerCase(), index);
                orsearch = andsearch;
                nextTerm++;
            }
            else {
                String term = terms[0].toLowerCase();
                if (term.endsWith("ed")) {
                    term = term.substring(0, term.length()-2);
                }
                if (term.endsWith("ing")) {
                    term = term.substring(0, term.length()-3);
                }
                andsearch = WikiSearch.search(term, index);
                orsearch = andsearch;
            }

            System.out.println(terms.length);
            for (int i=nextTerm; i<terms.length; i++) {
                if (TermCounter.stopwords.contains(terms[i].toLowerCase())) {
                    System.out.println(terms[i].toLowerCase());
                }
                else {
                    String term = terms[i].toLowerCase();
                    if (term.endsWith("ed")) {
                        term = term.substring(0, term.length()-2);
                    }
                    if (term.endsWith("ing")) {
                        term = term.substring(0, term.length()-3);
                    }
                    if (term.endsWith("s")) {
                        term = term.substring(0, term.length()-1);
                    }
                    andsearch = WikiSearch.search(term, index);
                    orsearch = andsearch;
                    WikiSearch extrasearch = WikiSearch.search(term, index);
                    andsearch = andsearch.and(extrasearch);
                    orsearch = orsearch.or(extrasearch);
                }
            }
            List<Map.Entry<String, Double>> bothentries = andsearch.sort();
            List<Map.Entry<String, Double>> oneentries = orsearch.sort();

            // add titles rather than links to show in results
            List<Map.Entry<String, Map.Entry<String, Double>>> titles = new LinkedList<>();
            for (Map.Entry<String, Double> entry: bothentries) {
                String title = entry.getKey().replaceAll("https://en.wikipedia.org/wiki/", "");
                title = title.replaceAll("_", " ");
                titles.add(new AbstractMap.SimpleEntry<>(title, entry));
            }

            // articles with only one of the terms will be less relevant, so later in the list results
            for (Map.Entry<String, Double> entry: oneentries) {
                if (!bothentries.contains(entry)) {
                    String title = entry.getKey().replaceAll("https://en.wikipedia.org/wiki/", "");
                    title = title.replaceAll("_", " ");
                    titles.add(new AbstractMap.SimpleEntry<>(title, entry));
                }
            }
            Map map = new HashMap();
            map.put("term", req.params(":term"));
            map.put("titles", titles);
            map.put("listsize", titles.size());
            return new ModelAndView(map, "results.hbs");
        }, new HandlebarsTemplateEngine());

        get("/", (req, res) -> {
//            crawl(wc);
            Map map = new HashMap();
            return new ModelAndView(map, "search.hbs");
        }, new HandlebarsTemplateEngine());

        post("/search", (req, res) -> {
            res.redirect("/search/"+req.queryParams("term"));
            return null;
        });
    }
}
