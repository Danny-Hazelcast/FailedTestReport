import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.rendersnake.HtmlCanvas;

/**
 * Created by danny on 2/10/14.
 */
public class HtmlUnitScraperOld {

    private static final String userName = "";
    private static final String password = "";
    private static final String clowedBeezzURL = "";

    private static final String broken = "Broken";

    static WebClient webClient = new WebClient(BrowserVersion.FIREFOX_17);

    static Random random = new Random();

    static BuiltTarget[] targets =  {

            new BuiltTarget("Hazelcast-3.x", 832, 862),
            new BuiltTarget("Hazelcast-3.x-nightly", 111, 121),

            new BuiltTarget("Hazelcast-3.x-IbmJDK1.6", 105, 116),
            new BuiltTarget("Hazelcast-3.x-IbmJDK1.7", 109, 122),

            new BuiltTarget("Hazelcast-3.x-OpenJDK6", 100, 112),
            new BuiltTarget("Hazelcast-3.x-OpenJDK7", 100, 113),
            new BuiltTarget("Hazelcast-3.x-OpenJDK8", 113, 127),


            new BuiltTarget("Hazelcast-3.x-OracleJDK1.6", 100, 112), //64

            //new BuiltTarget("Hazelcast-3.x-problematicTest", 64, 74),

    };

    public static void main(String[] args) throws IOException, InterruptedException {

        detailedTables(args);
    }

    public static void detailedTables(String[] args) throws IOException, InterruptedException {

        login();

        Writer writer = new FileWriter("HzBuilds.html");
        HtmlCanvas html = new HtmlCanvas(writer);

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        html.h1(html.attributes().align("center")).write("Created: "+dateFormat.format(new Date()))._h1();
        html.br();
        html.br();

        HashMap<String, Stats> AllFailedTests = new HashMap<String, Stats>();

        HashMap<String, CrossBuildStats> failedTestCrossBuild = new HashMap();


        for(BuiltTarget bt : targets){

            HashMap<String, Stats> failedTests = new HashMap<String, Stats>();
            HashMap<String, Stats> errorTests = new HashMap<String, Stats>();

            for ( int buildNum = bt.fromBuild; buildNum <= bt.toBuild; buildNum++){

                Thread.sleep( 1000 * (random.nextInt(3)+1) );

                try{
                    BufferedReader page = checkPage(bt.buildName, buildNum);
                    Date buildDate = findFailedTests(page, buildNum, failedTests, errorTests);

                    setDateOfBuildForTest(failedTests, buildNum, buildDate);
                    setDateOfBuildForTest(errorTests, buildNum, buildDate);

                    setNameOfBuildForTest(failedTests, buildNum, bt.buildName);
                    setNameOfBuildForTest(errorTests, buildNum, bt.buildName);


                    System.out.println("build "+buildNum+" "+buildDate);

                }catch(FailingHttpStatusCodeException e){
                    System.out.println("catched "+bt.buildName+" "+buildNum);
                    System.out.println( e );
                }

            }


            for(Map.Entry<String, Stats> entry : failedTests.entrySet()){
                mergeFailedTestsFromDiffBuilds(AllFailedTests, entry.getKey(), entry.getValue());

                mergeCrossBuild(failedTestCrossBuild, bt.buildName, entry.getKey(), entry.getValue());
            }

            for(Map.Entry<String, Stats> entry : errorTests.entrySet()){
                mergeFailedTestsFromDiffBuilds(AllFailedTests, entry.getKey(), entry.getValue());

                mergeCrossBuild(failedTestCrossBuild, bt.buildName, entry.getKey(), entry.getValue());
            }


            String out = getResultsString(bt.buildName, bt.fromBuild, bt.toBuild, failedTests, errorTests);
            System.out.println(out);

            //snake(html, bt.buildName, bt.fromBuild, bt.toBuild, failedTests, errorTests);
            //html.br();
            //html.br();

            Thread.sleep( 1000 * (random.nextInt(3)+1) );
        }

        String builds = new String();
        for(BuiltTarget bt : targets){
            builds += bt.buildName+", ";
        }

        snakeOut2(html, AllFailedTests, builds);


        writer.flush();
        writer.close();
    }


    static void setDateOfBuildForTest(HashMap<String, Stats> tests, int buildNum, Date date){
        for(Stats s : tests.values()){
            s.setDateOfBuild(buildNum, date);
        }
    }
    static void setNameOfBuildForTest(HashMap<String, Stats> tests, int buildNum, String name){
        for(Stats s : tests.values()){
            s.setBuildName(buildNum, name );
        }
    }

    static class BuiltTarget{
        public String buildName;
        public int fromBuild;
        public int toBuild;

        BuiltTarget(String buildName, int fromBuild, int toBuild) {
            this.buildName = buildName;
            this.fromBuild = fromBuild;
            this.toBuild = toBuild;
        }
    }


    static public String makeFileName(String name, int version){
        char oldCh = '.';
        char newCh = '-';
        return name.replace(oldCh, newCh)+"_"+version+".csv";
    }

    static String getResultsString(String buildName, int fromBuild, int toBuild, HashMap<String, Stats> failedTests, HashMap<String, Stats> errorTests){

        removeBroken(failedTests);//just getring rid of the odd broken lines of the out put
        removeBroken(errorTests);

        String out = "Build: "+buildName+" From: "+fromBuild+ " TO: "+toBuild+" TotalBuilds: "+ (toBuild-fromBuild) +"\n\n";

        out += "Failed Tests:\n";
        out += getFailedString(failedTests);

        out += "\n";

        out += "Error  Tests:\n";
        out += getFailedString(errorTests);

        return out;
    }


    static String getFailedString(HashMap<String, Stats> map){

        //FreqComparator bvc =  new FreqComparator(map);
        LastBuildComparator bvc =  new LastBuildComparator(map);


        TreeMap<String,Stats> sorted_map = new TreeMap<String,Stats>(bvc);
        sorted_map.putAll(map);

        String out = new String();

        out += "Test, Frequency, Build#, reasons#"+"\n";

        for(Map.Entry<String,Stats> t : sorted_map.entrySet()){

            Stats stats = t.getValue();

            out += t.getKey()+", "+stats.count+", ";
            for(BuildNoDate b : stats.builds){
                out += b.buildNum+" ";
            }
            out += ", "+stats.reasons.size();
            out += ", "+stats.getOrderedReasons();

            out +="\n";

        }
        return out;
    }


    static class FreqComparator implements Comparator<String> {

        Map<String, Stats> base;
        public FreqComparator(Map<String, Stats> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b) {
            if (base.get(a).count >= base.get(b).count) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }

    static class LastBuildComparator implements Comparator<String> {

        Map<String, Stats> base;
        public LastBuildComparator(Map<String, Stats> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b) {
            if (base.get(a).lastFail >= base.get(b).lastFail) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }

    static class IntegerValueComparator implements Comparator<String> {

        Map<String, Integer> base;
        public IntegerValueComparator(Map<String, Integer> base) {
            this.base = base;
        }

        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
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


    public static Date findFailedTests(BufferedReader in, int buildNumber, HashMap<String, Stats> failedTests, HashMap<String, Stats> errorTests) throws IOException {
        Date dateString = null;
        String line;
        while ( (line = in.readLine()) !=null ) {

            if(line.matches("\\[INFO\\] Finished at:.*")){

                System.out.println(line);
                dateString = getBuildDate(line);
            }

            if(line.matches("Results :")){

                while ( (line = in.readLine()) !=null  ) {

                    if ( line.matches("Tests run:.*") ) {
                        break;
                    }

                    if ( line.matches("Failed tests: " ) ) {

                        while ( ! (line = in.readLine()).matches("") ) {

                            FailedTest test = getFailedTest(line);

                            addTest(test, failedTests, buildNumber);
                        }
                    }

                    if(line.matches("Tests in error: ")){

                        while ( ! (line = in.readLine()).matches("") ) {

                            FailedTest test = getFailedTest(line);

                            addTest(test, errorTests, buildNumber);
                        }
                    }
                }
            }
        }
        return dateString;
    }

    public static FailedTest getFailedTest(String line){

        StringTokenizer st = new StringTokenizer(line, ":", false);

        if(st.hasMoreTokens()){

            String test = st.nextToken();

            if(st.hasMoreTokens()){
                String reason = st.nextToken();
                test = test.trim();

                if ( test.matches("^[A-Z].+") ) {
                    System.out.println(test);
                    return new FailedTest(test, reason);
                }

                return new FailedTest(broken, line);
            }
            else{
                return new FailedTest(broken, line);
            }

        }
        return new FailedTest(broken, line);
    }

    public static Date getBuildDate(String line){

        String dateInString = line.substring(line.lastIndexOf("at:")+4);


        System.out.println(dateInString);
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
        Date date = null;
        try {
            date = formatter.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }


    public static void mergeFailedTestsFromDiffBuilds(HashMap<String, Stats> map, String test, Stats stats){

        if(map.containsKey(test)){
            Stats totalStats = map.get(test);
            totalStats.count += stats.count;
            totalStats.builds.addAll(stats.builds);
        }else{
            map.put(test, stats);
        }
    }

    public static void mergeCrossBuild(HashMap<String, CrossBuildStats> map, String bulidName, String test, Stats stats){

        if(map.containsKey(test)){
            CrossBuildStats cross = map.get(test);
            cross.buildStats.put(bulidName, stats);
        }else{
            CrossBuildStats cross = new CrossBuildStats();
            cross.buildStats.put(bulidName, stats);
            map.put(test,  cross);
        }
    }



    public static void removeBroken(HashMap<String, Stats> map){ map.remove(broken); }

    public static void addTest(FailedTest testObj, HashMap<String, Stats> map, int buildNumber){

        String test = testObj.name;
        String reason = testObj.reason;

        if(map.containsKey(test)){
            Stats stat = map.get(test);
            stat.count++;
            stat.lastFail = buildNumber;
            stat.addReason(reason);
            stat.addFailedBuild(buildNumber);
            map.put(test, stat);
        }else{
            map.put(test, new Stats(buildNumber, reason));
        }
    }

    public static class BuildNoDate{
        public int buildNum;
        public String buildName;
        public Date date = null;

        BuildNoDate(int num){
            buildNum = num;
        }
    }

    public static class CrossBuildStats{
        public HashMap<String, Stats> buildStats = new HashMap();
    }

    public static class Stats{
        public int count=1;
        public int lastFail;
        public HashMap<String, Integer> reasons = new HashMap<String, Integer>();

        public List<BuildNoDate> builds = new ArrayList();


        public Stats(int lastFail, String reason){
            this.lastFail = lastFail;
            this.addReason(reason);
            this.addFailedBuild(lastFail);
        }

        public void addReason(String reason){
            if(reasons.containsKey(reason)){
                int count = reasons.get(reason);
                reasons.put(reason, ++count);
            }else{
                reasons.put(reason, 1);
            }
        }

        public void addFailedBuild(int build){
            //builds.add(build);
            builds.add(0, new BuildNoDate(build) );
        }

        public void setDateOfBuild(int build, Date date){
            for(BuildNoDate b : builds){
                if(b.buildNum == build && b.date == null){
                    b.date = date;
                }
            }
        }

        public void setBuildName(int build, String name){
            for(BuildNoDate b : builds){
                if(b.buildName == null){
                    b.buildName = name;
                }
            }
        }

        public BuildNoDate getOldestFail(){

            BuildNoDate old = builds.get(0);
            for(BuildNoDate b: builds){

                if(b.date.before(old.date)){
                    old = b;
                }

            }
            return old;
        }

        public BuildNoDate getNewestFail(){
            BuildNoDate newest = builds.get(0);
            for(BuildNoDate b: builds){

                if(b.date.after(newest.date)){
                    newest = b;
                }

            }
            return newest;
        }

        public String getOrderedReasons(){

            IntegerValueComparator bvc =  new IntegerValueComparator(reasons);
            TreeMap<String, Integer> sorted_map = new TreeMap<String, Integer>(bvc);
            sorted_map.putAll(reasons);

            String out = new String();
            for(Map.Entry<String,Integer> t : sorted_map.entrySet()){
                out += "[ "+t.getKey()+" ("+t.getValue()+") ] ";
            }
            return out;
        }
    }

    public static class FailedTest{
        public String name;
        public String reason;


        FailedTest(String name, String reason){
            this.name = name.trim();
            this.reason = reason.trim();

        }

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
        f.getInputByName("Name").setValueAttribute(userName);
        f.getInputByName("password").setValueAttribute(password);

        List<HtmlButton> b = (List<HtmlButton>) f.getByXPath("//button[@type='submit' and @value='Login']" );

        HtmlPage p1 = b.get(0).click();
        webClient.waitForBackgroundJavaScript(8000);
    }




    public static  void writeToFile(String content, String fileName) {
        try {

            File file = new File(fileName);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

            System.out.println(fileName+" write");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static public void snake(HtmlCanvas html, String buildName, int fromBuild, int toBuild, HashMap<String, Stats> failedTests, HashMap<String, Stats> errorTests) throws IOException{

        removeBroken(failedTests);//just getring rid of the odd broken lines of the out put
        removeBroken(errorTests);

        String title = "Build: "+buildName+" From: "+fromBuild+ " TO: "+toBuild+" TotalBuilds: "+ (toBuild-fromBuild) +"\n\n";

        html.h2().write(title)._h2()

        .h3()
        .write("Failed Tests:\n")
        ._h3();

        makeTable(html, failedTests);

        html.h3()
            .write("Error Tests:\n")
            ._h3();

        makeTable(html, errorTests);
    }

    static public void snakeOut2(HtmlCanvas html, HashMap<String, Stats> tests, String builds) throws IOException{

        removeBroken(tests);//just getring rid of the odd broken lines of the out put

        Date oldest=null;
        Date newest=null;

        for(Map.Entry<String,Stats> t : tests.entrySet()){
            Stats stats = t.getValue();
            if(oldest==null){
                oldest = stats.getOldestFail().date;
            }
            if(newest==null){
                newest = stats.getNewestFail().date;
            }

            Date o = stats.getOldestFail().date;
            if(o.before(oldest)){
                oldest = o;
            }

            Date n = stats.getNewestFail().date;
            if(n.after(newest)){
                newest = n;
            }

        }

        String title = "Oldest Fail: "+oldest+ "  Newest Fail: "+newest;
        html.h2().write(title)._h2();
        html.h4().write("builds "+builds)._h4();


        makeOverViewTable(html, tests);
    }

    static public void makeOverViewTable(HtmlCanvas html, HashMap<String, Stats> map) throws IOException {

        LastBuildComparator bvc =  new LastBuildComparator(map);
        TreeMap<String,Stats> sorted_map = new TreeMap<String,Stats>(bvc);
        sorted_map.putAll(map);

        html.table(html.attributes().border("1").cellpadding("6"))

                .tr()
                .th().write("Test Name")._th()
                .th().write("Frequency")._th()
                .th().write("failed in Builds")._th()
                .th().write("Oldest")._th()
                .th().write("Newest")._th()

                ._tr();


        for(Map.Entry<String,Stats> t : sorted_map.entrySet()){

            Stats stats = t.getValue();

            html.tr()
                    .td().write(t.getKey())._td()
                    .td().write(stats.count + "")._td();

            String builds=new String();
            for(BuildNoDate b : stats.builds){
                builds += b.buildName+" ";
            }

            html.td().write(builds)._td();

            html.td().write( stats.getOldestFail().date.toString() )._td();
            html.td().write( stats.getNewestFail().date.toString() )._td()


            ._tr();
        }

        html._table();
    }

    static public void makeTable(HtmlCanvas html, HashMap<String, Stats> map) throws IOException {

        LastBuildComparator bvc =  new LastBuildComparator(map);
        TreeMap<String,Stats> sorted_map = new TreeMap<String,Stats>(bvc);
        sorted_map.putAll(map);

        html.table(html.attributes().border("1").cellpadding("6"))

            .tr()
                .th().write("Test Name")._th()
                .th().write("Frequency")._th()
                .th().write("failed in Builds")._th()
                .th().write("diff reasons count")._th()
                .th().write("reasons")._th()
                .th().write("Oldest")._th()
                .th().write("Newest")._th()
            ._tr();


            for(Map.Entry<String,Stats> t : sorted_map.entrySet()){

                Stats stats = t.getValue();

                html.tr()
                        .td().write(t.getKey())._td()
                        .td().write(stats.count + "")._td();

                String builds=new String();
                for(BuildNoDate b : stats.builds){
                    builds += b.buildNum+" ";
                }

                html.td().write(builds)._td();

                html.td().write( stats.reasons.size()+"" )._td();
                html.td().write( stats.getOrderedReasons() )._td();
                html.td().write( stats.getOldestFail().date.toString() )._td();
                html.td().write( stats.getNewestFail().date.toString() )._td()
                        ._tr();
            }

        html._table();
    }

}