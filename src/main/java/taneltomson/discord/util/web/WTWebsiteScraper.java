package taneltomson.discord.util.web;


import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import taneltomson.discord.util.web.data.MemberInfo;
import taneltomson.discord.util.web.data.PlayedMatch;
import taneltomson.discord.util.web.data.SquadronInfo;


@Slf4j
public class WTWebsiteScraper {
    public static final String THC = "Try%20Hard%20Coalition";

    private static final String BASE_URL = "https://warthunder.com/en";
    private static final String SQUADRONS_URL =
            BASE_URL + "/community/getclansleaderboard/dif/_hist/page/{nr}/sort/dr_era5";
    private static final String MEMBER_INFOS_URL =
            BASE_URL + "/community/claninfo/";


    public List<MemberInfo> getSquadronMembersInfo(String squadronLongName) throws IOException {
        final List<MemberInfo> memberInfos = new ArrayList<>();

        final Document doc = Jsoup.connect(MEMBER_INFOS_URL + squadronLongName).get();
        for (Element tr : doc.select("table.clan-members tr")) {
            // log.debug("row column size: {}", tr.getAllElements().size());
            if (tr.getAllElements().size() != 9) {
                continue;
            }

            // log.debug("Parsing row: {}", tr);
            final MemberInfo memberInfo = new MemberInfo(
                    tr.getAllElements().get(3).text(),
                    Integer.valueOf(tr.getAllElements().get(5).text()),
                    tr.getAllElements().get(7).text(),
                    LocalDate.parse(tr.getAllElements().get(8).text(),
                                    DateTimeFormatter.ofPattern("d.MM.yyyy")));
            // log.debug("Created memberInfo: {}", memberInfo);
            memberInfos.add(memberInfo);
        }

        return memberInfos;
    }

    public SquadronInfo scrapeSquadronInfo(String squadronShortName)
            throws DataNotFoundException, IOException {
        for (int pageNr = 1; pageNr <= 10; pageNr++) {
            final String url = SQUADRONS_URL.replace("{nr}", String.valueOf(pageNr));
            log.debug("scrapeSquadronInfo - Scraping url:{}", url);

            final Document document = Jsoup.connect(url)
                                           .header("Accept", "*/*")
                                           .header("X-Requested-With", "XMLHttpRequest")
                                           .get();
            final JSONObject json = new JSONObject(document.text());
            if ("ok".equals(json.getString("status"))) {
                final JSONArray squadronsArray = json.getJSONArray("data");
                log.debug("scrapeSquadronInfo - squadronsArray.length(): {}",
                          squadronsArray.length());

                for (int i = 0; i < squadronsArray.length(); i++) {
                    final JSONObject squadronInfoJson = squadronsArray.getJSONObject(i);
                    final JSONObject squadronAStatsJson = squadronInfoJson.getJSONObject("astat");

                    final int position = squadronInfoJson.getInt("pos") + 1;
                    final int memberCount = squadronInfoJson.getInt("members_cnt");
                    final int squibPoints = squadronAStatsJson.getInt("dr_era5_hist");
                    final int airKills = squadronAStatsJson.getInt("akills_hist");
                    final int groundKills = squadronAStatsJson.getInt("gkills_hist");
                    final int deaths = squadronAStatsJson.getInt("deaths_hist");
                    final int durationInMins = squadronAStatsJson.getInt("ftime_hist");
                    final String fullName = squadronInfoJson.getString("name");
                    String shortName = squadronInfoJson.getString("lastPaidTag");
                    shortName = shortName.substring(1, shortName.length() - 1);

                    log.debug("scrapeSquadronInfo - Checking squadron with shortname: {}",
                              shortName);
                    if (squadronShortName.toLowerCase().equals(squadronInfoJson.getString("tagl")
                                                                               .toLowerCase())) {
                        final SquadronInfo squadronInfo = new SquadronInfo(
                                position, shortName, fullName, squibPoints,
                                memberCount, airKills, groundKills, deaths,
                                Duration.ofMinutes(durationInMins));
                        log.debug("scrapeSquadronInfo - returning squadron info: {}", squadronInfo);
                        return squadronInfo;
                    }
                }
            } else {
                throw new IOException("Response status not OK");
            }
        }

        log.error("scrapeSquadronInfo - Did not find info for squadron with shortName {}",
                  squadronShortName);
        throw new DataNotFoundException("Problem when scraping squadron info.");
    }


    public PlayedMatch getLastSquibsGames(String playerName) throws IOException {
        final String url = "https://warthunder.com"
                + "/en/tournament/replay/type/replays"
                + "?Filter[game_type][]=clanBattle"
                + "&Filter[keyword]="
                + "&Filter[nick]={playerName}"
                + "&action=search";

        final Connection.Response response = Jsoup.connect(url.replace("{playerName}", playerName))
                                                  .header("Accept", "*/*")
                                                  .method(Connection.Method.GET)
                                                  .execute();
        Document document = response.parse();

        log.debug("Ended up at URL: {}", document.location());
        if (document.location().contains("login")) {
            final FormElement form = (FormElement) document.select("form.login").first();

            form.select("input#email").val("tryhardsbot@gmail.com");
            form.select("input#password").val("R5fVU37P52A9SnN9Y5qAUpBl");

            final Document submittedForm = form.submit()
                                               .cookies(response.cookies())
                                               .method(Connection.Method.POST)
                                               .post();

            log.debug("After submitting login form ended up at URL: {}", submittedForm.location());
            if (!submittedForm.location().contains("replay")) {
                throw new RuntimeException("Loggin in failed.");
            }

            document = Jsoup.connect(url.replace("{playerName}", playerName))
                            .header("Accept", "*/*")
                            .method(Connection.Method.GET)
                            .cookies(response.cookies())
                            .execute()
                            .parse();
        }

        log.debug("Ended up at URL: {}", document.location());
        final Element match = document.select("div.replay-list a.replay__item").get(0);

        final Elements matchElems = match.select("div.col-9 div.row");

        String teams = matchElems.get(3).select("span.stat__value").text();
        String opponent = Arrays.asList(teams.split(" vs "))
                                .stream()
                                .filter(team -> !team.contains("xTHCx"))
                                .findFirst().get();
        opponent = opponent.substring(1, opponent.length() - 1);

        final String matchType = matchElems.get(0).select("div.replay__title").text();
        final LocalDateTime date = LocalDateTime
                .parse(matchElems.get(0).select("span.date__text").text(),
                       DateTimeFormatter.ofPattern("d MMMM yyyy kk:mm"));

        log.debug("matchType: {}, date: {}, opponent: {}", matchType, date, opponent);
        return new PlayedMatch(opponent, matchType, date);
    }
}
