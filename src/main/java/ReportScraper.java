import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.io.IOUtils;
import org.rendersnake.HtmlCanvas;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.rendersnake.HtmlAttributesFactory.href;

public class ReportScraper {
    private static final String userName = "";
    private static final String password = "";
    private static final String clowedBeezzURL = "https://hazelcast-l337.ci.cloudbees.com/job/";

    private static final String broken = "Broken";

    private static final List<FailedTest> failedTests = new ArrayList();


    static WebClient webClient = new WebClient(BrowserVersion.FIREFOX_17);

    static Random random = new Random();

    static String buildNamePreFix = "Hazelcast-";

    static BuiltTarget[] targets =  {


            new BuiltTarget("Hazelcast-3.maintenance", 282, 292),

            new BuiltTarget("Hazelcast-3.x", 1094, 1104),
            new BuiltTarget("Hazelcast-3.x-nightly", 179, 189),

            new BuiltTarget("Hazelcast-3.x-IbmJDK1.6", 168, 178),
            new BuiltTarget("Hazelcast-3.x-IbmJDK1.7", 178, 188),

            new BuiltTarget("Hazelcast-3.x-OpenJDK6", 160, 170),
            new BuiltTarget("Hazelcast-3.x-OpenJDK7", 165, 175),
            new BuiltTarget("Hazelcast-3.x-OpenJDK8", 189, 199),

            new BuiltTarget("Hazelcast-3.x-OracleJDK1.6", 163, 173),
            new BuiltTarget("Hazelcast-3.x-OracleJDK8", 61, 71),


            new BuiltTarget("Hazelcast-3.x-problematicTest", 123, 133)
    };

    public static void main(String[] args) throws IOException, InterruptedException {

        login();

        for(BuiltTarget bt : targets){
            ReportLine.buildNames.add(bt);

            for ( int buildNum = bt.fromBuild; buildNum <= bt.toBuild; buildNum++){
                Thread.sleep( 1000 * (random.nextInt(3)+1) );
                try{
                    BufferedReader page = checkPage(bt.buildName, buildNum);
                    Date buildDate = findFailedTests(page, bt.buildName, buildNum);

                    for(FailedTest f : failedTests)
                        if(f.date==null)
                            f.date = buildDate;

                }catch(FailingHttpStatusCodeException e){
                    System.out.println( "WHAT!! "+bt.buildName+" "+buildNum + e );
                }
            }
            Thread.sleep( 1000 * (random.nextInt(3)+1) );
        }
        System.out.println("==Scrape Done==");


        ListMultimap<String, FailedTest> allTests = ArrayListMultimap.create();
        for(FailedTest f : failedTests){
            allTests.put(f.testName, f);
        }

        List<ReportLine> report = new ArrayList();

        for(String testName : allTests.keySet()){
            ReportLine reportLine = new ReportLine();
            report.add(reportLine);
            reportLine.testName = testName;
            reportLine.totalFailsCount = allTests.get(testName).size();


            System.out.println(testName + " total Fails "+allTests.get(testName).size());


            ListMultimap<String, FailedTest> byBuild = ArrayListMultimap.create();
            Collection<FailedTest> failingInstances = allTests.get(testName);

            for(FailedTest t : failingInstances){
                byBuild.put(t.buildName, t);
            }

            List<FailedTest> max = new ArrayList();
            List<FailedTest> min = new ArrayList();

            for(String buildName : byBuild.keySet()){
                List<FailedTest> failedInBuild = byBuild.get(buildName);

                System.out.println(buildName + " = "+ failedInBuild.size());
                reportLine.buildsFailCount.put(buildName, failedInBuild.size() );


                for(FailedTest failedTestInstance : failedInBuild ){
                    reportLine.failedBuildNumbers.put(buildName, failedTestInstance.buildNumber);
                }

                max.add( Collections.max(failedInBuild, new FailedDateComp()) );
                min.add( Collections.min(failedInBuild, new FailedDateComp()) );
            }

            reportLine.last = Collections.max(max, new FailedDateComp()).date;
            reportLine.first = Collections.min(min, new FailedDateComp()).date;
        }


        Date from = Collections.min(report, new LineFirstDate()).first;
        Date to = Collections.max(report, new LineLastDate()).last;

        Collections.sort(report, new LineTotalFails() );


        Writer writer = new FileWriter("HzBuilds.html");
        HtmlCanvas html = new HtmlCanvas(writer);

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy/ HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");


        int total=0;
        for(BuiltTarget bt : ReportLine.buildNames){
            total += (bt.toBuild - bt.fromBuild);
        }


        html.h3(html.attributes().align("center")).write("Hazelcast failing tests - From: "+sdf.format(from)+"  To: "+sdf.format(to))._h3();
        html.h5(html.attributes().align("center")).write("Builds processed: "+total+ "  Created on: "+dateFormat.format(new Date()))._h5();
        html.br();

        html.table(html.attributes().border("1").cellpadding("6"));
            html.tr();
                html.th().write("Test Name")._th();
                html.th().write("Frequency")._th();

                html.th().write("First Fail")._th();
                html.th().write("Elapsed days")._th();
                html.th().write("Last Fail")._th();

                for(BuiltTarget bt : ReportLine.buildNames){
                    html.th().write(bt.buildName.replace(buildNamePreFix, "") + " (" + (bt.toBuild - bt.fromBuild) + ")")._th();
                }

            html._tr();

            for(ReportLine line : report){
                html.tr();
                    html.td().write(line.testName)._td();
                    html.td().write(line.totalFailsCount+"")._td();

                    html.td().write(sdf.format(line.first))._td();

                    long diff = line.last.getTime() - line.first.getTime();
                    long diffDays = diff / (24 * 60 * 60 * 1000);
                    html.td().write( diffDays+"" )._td();

                    html.td().write(sdf.format(line.last))._td();


                    for(BuiltTarget bt : ReportLine.buildNames){

                        Integer count = line.buildsFailCount.get(bt.buildName);

                        if(count!=null){
                            html.th();

                                html.write(count + "=[ ");
                                for(Integer buildnumber : line.failedBuildNumbers.get(bt.buildName) ){

                                    html.a(href(clowedBeezzURL + bt.buildName + "/" + buildnumber)).content(buildnumber + ", ");

                                }
                                html.write("] ");

                            html._th();

                        }else{
                            html.th().write("")._th();
                        }
                    }
                html._tr();
            }
        html._table();

        writer.flush();
        writer.close();
    }


    public static class BuiltTarget{
        public String buildName;
        public int fromBuild;
        public int toBuild;
        public int BildFails;

        BuiltTarget(String buildName, int fromBuild, int toBuild) {
            this.buildName = buildName;
            this.fromBuild = fromBuild;
            this.toBuild = toBuild;
        }
    }

    public static class FailedTest{
        public String buildName;
        public int buildNumber;
        public Date date = null;
        public String testName;
        public String reason;
        public boolean fail;

        FailedTest(String testName, String reason, String buildName, int buildNumber, boolean fail){
            this.testName = testName.trim();
            this.reason = reason.trim();
            this.buildName = buildName.trim();
            this.buildNumber = buildNumber;
            this.fail = fail;
        }
    }

    static class FailedDateComp implements Comparator<FailedTest> {
        public int compare(FailedTest a, FailedTest b) {
            if (a.date.before(b.date) ) {
                return -1;
            } else {
                return 1;
            }
        }
    }


    public static class ReportLine {
        public static List<BuiltTarget> buildNames = new ArrayList();
        public Map<String, Integer> buildsFailCount = new HashMap();
        public ListMultimap<String, Integer> failedBuildNumbers = ArrayListMultimap.create();


        public String testName;
        public int totalFailsCount;

        public Date first;
        public Date last;
    }

    static class LineTotalFails implements Comparator<ReportLine> {

        public int compare(ReportLine a, ReportLine b) {
            if (a.totalFailsCount > b.totalFailsCount ) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    static class LineFirstDate implements Comparator<ReportLine> {
        public int compare(ReportLine a, ReportLine b) {
            if (a.first.before(b.first) ) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    static class LineLastDate implements Comparator<ReportLine> {
        public int compare(ReportLine a, ReportLine b) {
            if (a.last.before(b.last) ) {
                return -1;
            } else {
                return 1;
            }
        }
    }


    public static BufferedReader checkPage(String jobName, int idx) throws IOException, FailingHttpStatusCodeException {
        String url = clowedBeezzURL + jobName + "/" + idx + "/consoleText";
        Page res = webClient.getPage(url);
        System.out.println(url);
        String s = res.getWebResponse().getContentAsString();
        InputStream inputStream = IOUtils.toInputStream(s, "UTF-8");
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        return in;
    }

    public static Date findFailedTests(BufferedReader in, String buildName, int buildNumber) throws IOException {
        Date dateString = null;
        String line;
        while ( (line = in.readLine()) !=null ) {
            if(line.matches("\\[INFO\\] Finished at:.*")){
                dateString = getBuildDate(line);
            }

            if(line.matches("Results :")){
                while ( (line = in.readLine()) !=null  ) {
                    if ( line.matches("Tests run:.*") ) {
                        break;
                    }
                    if ( line.matches("Failed tests: " ) ) {
                        while ( ! (line = in.readLine()).matches("") ) {
                            FailedTest test = getFailedTest(line, buildName, buildNumber, true);
                            if(test!=null){
                                failedTests.add(test);
                            }
                        }
                    }
                    if(line.matches("Tests in error: ")){
                        while ( ! (line = in.readLine()).matches("") ) {
                            FailedTest test = getFailedTest(line, buildName, buildNumber, false);
                            if(test!=null){
                                failedTests.add(test);
                            }
                        }
                    }
                }
            }
        }
        return dateString;
    }

    public static FailedTest getFailedTest(String line, String buildName, int bulidNumber, boolean failed){
        StringTokenizer st = new StringTokenizer(line, ":", false);
        if(st.hasMoreTokens()){
            String test = st.nextToken();
            if(st.hasMoreTokens()){
                String reason = st.nextToken();
                test = test.trim();
                if ( test.matches("^[A-Z].+") ) {
                    System.out.println(test);
                    return new FailedTest(test, reason, buildName, bulidNumber, failed);
                }
                return null;  //new FailedTest(broken, line, buildName, bulidNumber, failed);
            }
            else{
                return null; //new FailedTest(broken, line, buildName, bulidNumber, failed);
            }
        }
        return null; //new FailedTest(broken, line, buildName, bulidNumber, failed);
    }

    public static Date getBuildDate(String line){
        String dateInString = line.substring(line.lastIndexOf("at:")+4);
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
        Date date = null;
        try {
            date = formatter.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static void login() throws IOException {
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getCookieManager().setCookiesEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);

        DefaultCredentialsProvider provider = new DefaultCredentialsProvider();

        provider.addCredentials(userName, password);
        webClient.setCredentialsProvider(provider);

        final HtmlPage startPage = webClient.getPage("https://grandcentral.cloudbees.com/login");

        HtmlForm f = startPage.getForms().get(0);
        f.getInputByName("name").setValueAttribute(userName);
        f.getInputByName("password").setValueAttribute(password);

        List<HtmlButton> b = (List<HtmlButton>) f.getByXPath("//button[@type='submit' and @value='Login']" );

        HtmlPage p1 = b.get(0).click();
        webClient.waitForBackgroundJavaScript(8000);
    }

}