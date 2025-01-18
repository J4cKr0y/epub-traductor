package com.J4cKr0y.epub_traductor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.domain.Resource;



public class EpubTranslator {

    private static final String API_URL = "https://api.reverso.net/translate/v1/translation";  

    public static void main(String[] args) {
        try {
            // Lire le fichier EPUB
            FileInputStream fileInputStream = new FileInputStream("resources/your_file.epub");
            InputStream is = new BufferedInputStream(fileInputStream);
            
            EpubReader epubReader = new EpubReader();
            Book book = epubReader.readEpub(is);

            // Traverser et traduire le contenu de chaque chapitre
            List<Resource> contents = book.getContents();
            for (Resource resource : contents) {
                String content = inputStreamToString(resource.getInputStream());
                String translatedContent = translateWithReverso("en", "fr", content);
                resource.setData(translatedContent.getBytes());
            }

            // Écrire le fichier EPUB traduit
            EpubWriter epubWriter = new EpubWriter();
            epubWriter.write(book, new FileOutputStream("translated_file.epub"));

            System.out.println("Traduction terminée avec succès!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static String translateWithReverso(String srcLang, String destLang, String text) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(API_URL);
            postRequest.setHeader("Content-type", "application/json");

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("source_lang", srcLang);
            requestBody.addProperty("target_lang", destLang);
            requestBody.addProperty("input", text);

            StringEntity entity = new StringEntity(requestBody.toString());
            postRequest.setEntity(entity);

            // Récupérer la réponse sous format String
            String responseString = EntityUtils.toString(httpClient.execute(postRequest).getEntity());

            // Vérifier si la réponse est un JSON valide
            if (responseString.trim().startsWith("<")) {
                System.out.println("Erreur : La réponse n'est pas un JSON valide.");
                System.out.println(responseString);
                return "Erreur dans la traduction";
            }

            // Traiter et analyser la réponse JSON avec Gson en mode "lenient"
            JsonReader jsonReader = new JsonReader(new StringReader(responseString));
            jsonReader.setLenient(true);

            JsonObject responseJson = JsonParser.parseReader(jsonReader).getAsJsonObject();

            return responseJson.getAsJsonArray("translation").get(0).getAsString();
        }
    }
}
