import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Worker implements Runnable {

    private final TreeMap<String, Utente> registeredList;
    private TreeMap<String, SocketAddress> usersList;
    private ServerService.Con con;
    private SelectionKey key;

    public Worker(ServerService.Con con,TreeMap<String, Utente> registeredList,
                  TreeMap<String, SocketAddress> usersList,SelectionKey key) {
        this.con  = con;
        this.registeredList = registeredList;
        this.usersList = usersList;
        this.key = key;
    }

    @Override
    public void run() {

        con.req.flip();
        String receive = StandardCharsets.UTF_8.decode(con.req).toString();
        System.out.println("ecco stringa ricevuta: "+receive);
        String[] elenco = receive.split("/");
        SocketChannel client =(SocketChannel) key.channel();


        if(elenco.length >0){
            switch (elenco[0]){
                case "login" : // typeOp 1
                    String username = elenco[1];
                    String password = elenco[2];

                    Utente utente= null;
                    synchronized (registeredList) {
                        if (registeredList.containsKey(username)) {
                            utente = registeredList.get(username);
                        }
                    }
                    String response="";
                    /*
                    *   response = 0 : login già effettuato;
                    *   response = 1 : login ok;
                    *   response = -1 : password sbagliata;
                    *   responde = -2 : utente non registrato;
                    * */
                    if(utente!=null){
                        if(password.equals(utente.getPassword())){
                            boolean ok=false;
                            synchronized (usersList){
                                if(usersList.containsKey(utente.getUsername())){
                                    ok = true;
                                } else {
                                    usersList.put(username,con.sa);
                                }
                            }

                            if(ok){
                                // login già effettuato

                                response += "0";

                            } else {
                                // login ok
                                response+= "1";
                            }
                        } else {
                            // password sbagliata
                            response+= "-1";
                        }
                    } else {
                        // utente non è registrato
                        response+= "-2";
                    }

                    // salvo risposta per client in con.resp
                    con.typeOp = 1;
                    con.resp = ByteBuffer.wrap(response.getBytes());

                    System.out.println(new String(con.resp.array()));
                    //key.attach(con);
                    //key.interestOps(SelectionKey.OP_WRITE);


                    try {
                        client.write(con.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                break;
                case "logout": // typeOp 2
                    String uname = elenco[1];

                    boolean ok = false;
                    synchronized (usersList){
                        if(usersList.containsKey(uname)){
                            usersList.remove(uname);
                            ok = true;
                        }
                    }

                    String resp ="";
                    /*
                    *   resp = 1 : logout ok;
                    *   resp = -1 : utente non era online.
                    *
                    * */
                    if(ok){
                        // logout ok = 1
                        resp += "1";
                    } else {
                        resp += "-1";
                    }

                    con.typeOp = 2;
                    con.resp = ByteBuffer.wrap(resp.getBytes());

                    System.out.println(new String(con.resp.array()));
                    //key.attach(con);
                    //key.interestOps(SelectionKey.OP_WRITE);



                    try {
                        client.write(con.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "lista_amici":
                    String user = elenco[1];

                    Utente u = null;
                    synchronized (registeredList){
                        u=registeredList.get(user);
                    }

                    if(u!=null){
                        Vector<String> friends = u.getFriends();

                        JSONArray array = new JSONArray();

                        Iterator<String> iterator = friends.iterator();
                        while(iterator.hasNext()){
                            JSONObject obj = new JSONObject();
                            obj.put("username",iterator.next());
                            array.add(obj);
                        }

                        String result = array.toJSONString();


                        con.resp = ByteBuffer.wrap(result.getBytes());

                        try {
                            client.write(con.resp);
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                        con.clearAll();
                        key.attach(con);
                    }
                break;
                case "aggiungi_amico":
                    String us = elenco[1];
                    String nfriend = elenco[2];

                    int isok = 0;
                    synchronized (registeredList){

                        if(registeredList.get(nfriend)!=null){

                            Vector<String> friends = registeredList.get(us).getFriends();
                            if(friends.contains(nfriend)){
                                isok= -2;
                            } else {

                                registeredList.get(us).setFriend(nfriend);
                                registeredList.get(nfriend).setFriend(us);
                                isok = 1;

                            }
                        } else {
                            isok= -1;
                        }
                    }

                    String risp ="";
                    /*
                    *   risp = 1: aggiungi amico ok;
                    *   risp = -1: amico non esistente;
                    *   risp = -2: amico precedentemente aggiunto.
                    *
                    * */

                    risp += isok;

                    con.resp = ByteBuffer.wrap(risp.getBytes());

                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    ServerService.saveUsersStats(registeredList);
                break;
                case "mostra_punteggio":
                    String ute = elenco[1];

                    long punteggio = 0;

                    synchronized (registeredList){
                        punteggio= registeredList.get(ute).getPoint();
                    }

                    String risposta =""+ punteggio;

                    con.resp = ByteBuffer.wrap(risposta.getBytes());

                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "mostra_classifica":
                    String ut = elenco[1];
                    Map<String,Long> classifica = new TreeMap<String,Long>();

                    synchronized (registeredList){
                        classifica.put(ut,registeredList.get(ut).getPoint());

                        Vector<String> friends = registeredList.get(ut).getFriends();

                        Iterator<String> iterator = friends.iterator();

                        while(iterator.hasNext()){
                            String friend = iterator.next();
                            classifica.put(friend,registeredList.get(friend).getPoint());
                        }
                    }

                    SortedSet<Map.Entry<String,Long>> result = entriesSortedByValues(classifica);
                    Iterator<Map.Entry<String,Long>>iterator= result.iterator();
                    JSONArray array = new JSONArray();

                    while(iterator.hasNext()){
                        JSONObject obj = new JSONObject();
                        Map.Entry<String,Long> next = iterator.next();
                        obj.put(next.getKey(),next.getValue());
                        array.add(obj);
                    }

                    String rispo = array.toJSONString();

                    con.resp = ByteBuffer.wrap(rispo.getBytes());
                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "sfida":

                break;
                default:
                break;
            }
        }
        return;
    }

    static <K,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1;
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
