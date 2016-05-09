package me.urielsalis.isbnsearch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by urielsalis on 04/05/16.
 */
public class Main {
    public static Main main;
    public static Workbook workbook;
    public static Sheet sheet;
    public static int column = 1;
    public static int column2 = 2;
    public static String str;
    public static HttpClient httpclient;
    public static String temp;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) throw new Exception("Falta archivo de excel");
        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        if(args.length == 2) column = Integer.parseInt(args[1]);
        if(args.length == 3) str = args[2];
        if(args.length == 4) column2 = args[3];
        System.out.println(args[0]);
        if (args[0].endsWith(".xlsx") || args[0].endsWith(".xls")) {
            FileInputStream fis = new FileInputStream(args[0]);
            try {
                workbook = new HSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
            } finally {
                fis.close();
            }
        } else {
            throw new Exception("No es un archivo de excel");
        }
        httpclient = HttpClients.createDefault();

        main = new Main();
    }

    public Main() {
        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String name = row.getCell(0).getStringCellValue();
                name = name.substring(name.indexOf("(") - 1, line.indexOf(")") + 1);
                String isbn = getISBN(name);
                if(isbn != null) {
                    if(row.getCell(column) == null) row.createCell(column);
                    if(row.getCell(column2) == null) row.createCell(column2);
                    if(temp != null) { //temp is set to autor in searchISBN, null if not found, nullify when done
                        row.getCell(column).setCellValue(isbn);
                        row.getCell(column2).setCellValue(temp);
                        temp = null;
                    }
                }
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream("result.xls");
            workbook.write(fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getISBN(String name) {
        HttpPost httppost = new HttpPost("http://www.isbnargentina.org.ar/web/busqueda-avanzada-resultados.php");

        List<NameValuePair> params = new ArrayList<NameValuePair>(8);
        params.add(new BasicNameValuePair("ingresa", "1"));
        params.add(new BasicNameValuePair("titulo", name));
        params.add(new BasicNameValuePair("isbn", ""));
        params.add(new BasicNameValuePair("autor", ""));
        params.add(new BasicNameValuePair("sello", str));
        params.add(new BasicNameValuePair("fechad", ""));
        params.add(new BasicNameValuePair("fechah", ""));
        params.add(new BasicNameValuePair("enviar", "Buscar"));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            httppost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httppost.setHeader("Origin", "http://www.isbnargentina.org.ar");
            httppost.setHeader("Referer", "http://www.isbnargentina.org.ar/web/busqueda-simple.php");
            httppost.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36");

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if(entity != null) {
                InputStream instream = entity.getContent();
                try {
                    String line;
                    String isbn = null;
                    String autor = null;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(instream));
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("<a href=tmp/baja.php?file=")) {
                            String download = "http://www.isbnargentina.org.ar/web/" + line.substring(line.indexOf("<a href=") + 8, line.indexOf("> DESCARGAR RESULTADOS"));
                            System.out.println(download);
                            URL website = new URL(download);
                            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                            FileOutputStream fos = new FileOutputStream("download.csv");
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                            BufferedReader br = new BufferedReader(new FileReader("download.csv"));
                            Date maxDate = new Date(10);
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy");
                            String line2;
                            int sum = 0;
                            while ((line2 = br.readLine()) != null) {
                                //ISBN;TÍTULO;RAZÓN SOCIAL;EDICIÓN;AUTORES;PALABRAS CLAVES;PRECIO;FECHA DE PUBLICACIÓN
                                String[] lines = line2.split(";");
                                sum++;
                                if (!lines[0].equals("ISBN")) {
                                    if(lines.length > 6) {
                                        Date date = sdf.parse(lines[7]);
                                        if (date.after(maxDate)) {
                                            System.out.println(date);
                                            maxDate = date;
                                            isbn = lines[0];
                                            autor = lines[4];
                                            System.out.println(isbn);
                                        }
                                    }
                                }
                            }
                            temp = autor;
                            if(sum==1) return "NO";
                            break;

                        }
                    }
                    temp = autor;
                    return isbn;
                } catch (ParseException e) {
                    e.printStackTrace();
                } finally {
                    instream.close();
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
