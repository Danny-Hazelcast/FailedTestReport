import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by danny on 6/17/14.
 */
public class SuppressionScrap {

    public ListMultimap<String, String> allSuppressionsByRule = ArrayListMultimap.create();

    public ListMultimap<String, String> classSuppressionsByRule = ArrayListMultimap.create();
    public ListMultimap<String, String> pakageSuppressionsByRule = ArrayListMultimap.create();

    public ListMultimap<String, String> classSuppressionsByWildRule = ArrayListMultimap.create();
    public ListMultimap<String, String> pakageSuppressionsByWildRule = ArrayListMultimap.create();


    public ListMultimap<String, String> rulesByClass = ArrayListMultimap.create();
    public ListMultimap<String, String> rulesByPakage = ArrayListMultimap.create();


    public SuppressionScrap(String path){


        File fXmlFile = new File(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();


            NodeList nList = doc.getElementsByTagName("suppress");


            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    String rule =  eElement.getAttribute("checks");
                    String file =  eElement.getAttribute("files");

                    if(file.indexOf('[') == -1 ){
                        if(rule.equals("")){

                            classSuppressionsByWildRule.put(rule, file);

                        }else {
                            classSuppressionsByRule.put(rule, file);
                            rulesByClass.put(file, rule);
                        }

                    }else{
                        if(rule.equals("")){

                            pakageSuppressionsByWildRule.put(rule, file);

                        }else {
                            pakageSuppressionsByRule.put(rule, file);
                            rulesByPakage.put(file, rule);
                        }
                    }

                    allSuppressionsByRule.put(rule, file);
                }
            }

            Date now = new Date();
            System.out.println("=== Suppression stats "+now+"===");
            System.out.println();


            System.out.println("Total Suppression's                      = "+allSuppressionsByRule.size());
            System.out.println("Total Unique suppression's               = "+allSuppressionsByRule.keySet().size());

            System.out.println("Total Packages with wildcard suppression = "+pakageSuppressionsByWildRule.size());
            System.out.println("Total Packages with specific suppression = "+rulesByPakage.keySet().size());

            System.out.println("Total Classes with wildcard suppression  = "+classSuppressionsByWildRule.size());
            System.out.println("Total Classes with specific suppression  = "+rulesByClass.keySet().size());


            System.out.println();

            System.out.println("---Wild card Suppressed Packages ---");
            for(String k : pakageSuppressionsByWildRule.keySet()){
                for(String p : pakageSuppressionsByWildRule.get(k)){
                    System.out.println(p);
                }
            }
            System.out.println();


            System.out.println("---Suppressed Packages Ordered by Suppression instance count---");
            printMostBrokenRule(rulesByPakage);
            System.out.println();


            System.out.println("---Wild card Suppressed Classes ---");
            for(String k : classSuppressionsByWildRule.keySet()){
                for(String p : classSuppressionsByWildRule.get(k)){
                    System.out.println(p);
                }
            }
            System.out.println();


            System.out.println("---Suppressed Classes  Ordered by Suppression instance count---");
            printMostBrokenRule(rulesByClass);
            System.out.println();


            System.out.println("---Suppression's Ordered by most broken grouped by Package");
            printMostBrokenRule(pakageSuppressionsByRule);
            System.out.println();

            System.out.println("---Suppression's Ordered by most broken grouped by Classes");
            printMostBrokenRule(classSuppressionsByRule);
            System.out.println();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void printMostBrokenRule(ListMultimap<String, String> suppressionsByRule){

        List<BrokenRule> brokenRuleList = getOrderedRuleList(suppressionsByRule);

        int totalBrokenInstances=0;
        for(BrokenRule rule : brokenRuleList){
            System.out.println(rule);
            totalBrokenInstances+=rule.frequency;
        }

        System.out.println("total = "+brokenRuleList.size() +" instances = "+totalBrokenInstances);
    }


    private  List<BrokenRule> getOrderedRuleList(ListMultimap<String, String> suppressionsByRule){
        List<BrokenRule> brokenRuleList = new ArrayList();
        for(String rule : suppressionsByRule.keySet()){

            BrokenRule br = new BrokenRule();
            br.name = rule;
            br.frequency = suppressionsByRule.get(rule).size();

            brokenRuleList.add(br);
        }

        Collections.sort(brokenRuleList, new OriderByBroken());

        return brokenRuleList;
    }



    public class BrokenRule {
        public String name;
        public int frequency;

        @Override
        public String toString() {
            return name +"="+ frequency;
        }
    }

    static class OriderByBroken implements Comparator<BrokenRule> {

        public int compare(BrokenRule a, BrokenRule b) {
            if (a.frequency > b.frequency) {
                return -1;
            } else if (a.frequency < b.frequency) {
                return 1;
            }
            return 0;
        }
    }


    public static void main(String[] args){

        args = new String[1];
        args[0]="suppressions.xml";
        new SuppressionScrap(args[0]);
    }




}
