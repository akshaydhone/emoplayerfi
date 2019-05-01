package emoplayer;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.util.Pair;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


 
public class PlayList {
    
    private ObservableList<Pair<String,String>> songs = FXCollections.observableArrayList();
    private String url;
    public boolean flag;
    
    public void load(final String url) {
    	System.out.println("in PlayList load function");
    	flag = false;
    	System.out.println("in PlayList load function flag = false");
        this.url = url;
        
        //songs.clear();
        int cId = FxExperiencePlayer.curreentSongIndex;
        System.out.println("current song index : "+cId);
        int size = songs.size();
        
        System.out.println("Playlist size : "+ size);
        int delItems = size - (cId+1);
        System.out.println("delete songs : "+ delItems);
        int j=1;
        while(j<=delItems)
        {
        	songs.remove(cId+1);
        	System.out.println("deleted a song Playlist size : "+songs.size());
        	j++;
        }
        if (url.toLowerCase().endsWith(".mp3")) {
        	System.out.println("mp3 song to be added");
        	flag = true;
            songs.add(new Pair(
                url.substring(url.lastIndexOf('/'), url.lastIndexOf('.')),
                url
           
            ));
            
            System.out.println("in playlist load function notify");
            System.out.println("mp3 song added  "+songs.size());
        } else if (url.toLowerCase().endsWith(".m3u")) {
            loadM3U(url);
        } else if (url.toLowerCase().endsWith(".xml")) {
            loadPhlowXML(url);
        }
        System.out.println("exitting PlayList.load function");
        System.out.println("Playlist size : "+songs.size());
    }
    
    private void loadM3U(final String url) {
        final Task<List<Pair<String,String>>> fetchPlayListTask = new Task<List<Pair<String,String>>>(){
            @Override protected List<Pair<String,String>> call() throws Exception {
                System.out.println("call()");
                String baseUrl = url.substring(0,url.lastIndexOf('/')+1);
                System.out.println("baseUrl = " + baseUrl);
                List<Pair<String,String>> songs = new ArrayList();
                URLConnection con = new URL(url).openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(),"ISO-8859-1"));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                    if(inputLine.charAt(0) != '#') {
                        songs.add(new Pair(
                            inputLine.substring(0, inputLine.lastIndexOf('.')), 
                            baseUrl + URLEncoder.encode(inputLine, "UTF-8")
                        ));
                    }
                }
                in.close();
                System.out.println("content = " + Arrays.toString(songs.toArray()));
                return songs;
            }
        };
        fetchPlayListTask.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override public void changed(ObservableValue<? extends State> arg0, State oldState, State newState) {
                System.out.println("newState = " + newState);
                if (newState == State.SUCCEEDED) {
                    try {
                        songs.addAll(fetchPlayListTask.get());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    if (fetchPlayListTask.getException() != null) {
                        fetchPlayListTask.getException().printStackTrace();
                    }
                }
            }
        });
        new Thread(fetchPlayListTask).start();
    }
    
    private void loadPhlowXML(final String url) {
    	System.out.println("parsing the xml file ");
    	 System.out.println("Playlist size : "+songs.size());
        final Task<List<Pair<String,String>>> fetchPlayListTask = new Task<List<Pair<String,String>>>(){
            @Override protected List<Pair<String,String>> call() throws Exception {
                String baseUrl = url.substring(0,url.lastIndexOf('/')+1);
                List<Pair<String,String>> songs = new ArrayList();
                URLConnection con = new URL(url).openConnection();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(con.getInputStream());
                Element root = doc.getDocumentElement();
                NodeList children = root.getElementsByTagName("file");
                for(int i=0; i< children.getLength(); i++) {
                    Element child = (Element)children.item(i);
                    String name = child.getAttributes().getNamedItem("name").getTextContent();
                    if (name.endsWith(".mp3")) {
                        NodeList titleElements = child.getElementsByTagName("title");
                        String title;
                        if (titleElements.getLength() > 0) {
                            title = titleElements.item(0).getTextContent().trim();
                        } else {
                            title = name.substring(0, name.lastIndexOf('.'));
                        }
                        songs.add(new Pair(title,baseUrl + URLEncoder.encode(name, "UTF-8")));
                    }
                    
                }
               flag = true;
                System.out.println("in parsing xml notify");
                System.out.println("Playlist size : "+songs.size());
                return songs;
            }
           
        };
        fetchPlayListTask.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override public void changed(ObservableValue<? extends State> arg0, State oldState, State newState) {
                if (newState == State.SUCCEEDED) {
                    try {
                        songs.addAll(fetchPlayListTask.get());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    if (fetchPlayListTask.getException() != null) {
                        fetchPlayListTask.getException().printStackTrace();
                    }
                }
            }
        });
        new Thread(fetchPlayListTask).start();
    }

    public String getUrl() {
        return url;
    }

    public ObservableList<Pair<String,String>> getSongs() {
        return songs;
    }
}
