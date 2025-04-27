package ca.bungo.holos.utility;

import ca.bungo.holos.BungosHolos;
import org.bukkit.Color;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NetworkUtility {

    public static final String MOJANG_SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final boolean debug = false;

    public static CompletableFuture<Color[][]> getPlayerSkin(String playerUUID) throws URISyntaxException {
        if(debug) return getNetworkImage("https://i.bungo.ca/u/botgo.png");
        try(HttpClient client = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(MOJANG_SESSION_API + playerUUID + "?unsigned"))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body)
                    .thenApply(s -> {
                        try {
                            return new JSONParser().parse(s);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }).thenApply((json) -> {
                        JSONObject obj = (JSONObject) json;
                        JSONArray props = (JSONArray) obj.get("properties");
                        JSONObject object = (JSONObject) props.getFirst();
                        String textureB64 = (String) object.get("value");
                        try {
                            JSONObject textureObject = (JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(textureB64)));
                            JSONObject texture = (JSONObject) textureObject.get("textures");
                            JSONObject skin = (JSONObject) texture.get("SKIN");
                            String skinURL = (String) skin.get("url");
                            return getNetworkImage(skinURL).get();
                        } catch (ParseException | URISyntaxException | InterruptedException | ExecutionException e) {
                            BungosHolos.LOGGER.error("Failed to pull player skin... {}", e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public static CompletableFuture<Color[][]> getNetworkImage(String imageURL) throws URISyntaxException {
        try(HttpClient client = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(imageURL)).GET().build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(HttpResponse::body)
                    .thenApply(inputStream -> {
                        try {
                            if(inputStream != null){
                                BufferedImage image = ImageIO.read(inputStream);

                                if(image == null) return null;

                                Color[][] colors = new Color[image.getWidth()][image.getHeight()];
                                for(int x = 0; x < image.getWidth(); x++) {
                                    for(int y = 0; y < image.getHeight(); y++) {
                                        Color color = Color.fromARGB(image.getRGB(x, y));
                                        colors[x][image.getHeight() - 1 - y] = color;
                                    }
                                }
                                return colors;
                            }
                        } catch (IOException e) {
                            BungosHolos.LOGGER.error("Failed to load color map... {}", e.getMessage(), e);
                        }
                        return null;
                    });
        }
    }

}
