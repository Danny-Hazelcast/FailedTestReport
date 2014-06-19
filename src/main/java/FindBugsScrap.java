import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
public class FindBugsScrap {

    public ListMultimap<String, String> bugsByType = ArrayListMultimap.create();
    public ListMultimap<String, String> bugsBypriority = ArrayListMultimap.create();
    public ListMultimap<String, String> bugsBycategory = ArrayListMultimap.create();

    public ListMultimap<String, String> bugsByFile = ArrayListMultimap.create();


    public FindBugsScrap(String path){


        File fXmlFile = new File(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("BugInstance");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String type =  eElement.getAttribute("type");
                    String priority =  eElement.getAttribute("priority");
                    String category =  eElement.getAttribute("category");
                    String msg =  eElement.getAttribute("message");
                    String lineNumber =  eElement.getAttribute("lineNumber");

                    bugsByType.put(type, msg);
                    bugsBypriority.put(priority, msg);
                    bugsBycategory.put(category, msg);
                }
            }


            nList = doc.getElementsByTagName("file");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String classname =  eElement.getAttribute("classname");

                    NodeList child = eElement.getChildNodes();
                    for (int j = 0; j < child.getLength(); j++) {

                        Node childnode = child.item(j);

                        String msg =  eElement.getAttribute("message");
                        bugsByFile.put(classname, msg);
                    }
                }
            }



            Date now = new Date();
            System.out.println("=== FindBugs stats "+now+"===");
            System.out.println();

            System.out.println("Total Bugs's                      = "+ bugsByType.size());
            System.out.println();


            System.out.println("---bugs by priority---");
            for(NameCount n : orderByCount(bugsBypriority) ){
                System.out.println(n.name+"="+n.frequency);
            }
            System.out.println();

            System.out.println("---bugs by type---");
            for(NameCount n : orderByCount(bugsByType) ){
                System.out.println(n.name+"="+n.frequency);
            }
            System.out.println();

            System.out.println("---bugs by File---");
            for(NameCount n : orderByCount(bugsByFile) ){
                System.out.println(n.name+"="+n.frequency);
            }
            System.out.println();


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private  List<NameCount> orderByCount(ListMultimap<String, String> map){
        List<NameCount> list = new ArrayList();
        for(String keyName : map.keySet()){

            NameCount pair = new NameCount();
            pair.name = keyName;
            pair.frequency = map.get(keyName).size();

            list.add(pair);
        }

        Collections.sort(list, new OriderByCount());

        return list;
    }



    public class NameCount {
        public String name;
        public int frequency;

        @Override
        public String toString() {
            return name +"="+ frequency;
        }
    }

    static class OriderByCount implements Comparator<NameCount> {

        public int compare(NameCount a, NameCount b) {
            if (a.frequency > b.frequency) {
                return -1;
            } else if (a.frequency < b.frequency) {
                return 1;
            }
            return 0;
        }
    }


    public static void main(String[] args){

        //args = new String[1];
        //args[0] = "findbugs.xml";

        new FindBugsScrap(args[0]);
    }


}
