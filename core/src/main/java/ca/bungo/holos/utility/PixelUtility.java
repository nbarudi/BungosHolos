package ca.bungo.holos.utility;

import org.bukkit.Color;

public class PixelUtility {

    public static Color[][] extractRegion(Color[][] source, int x, int y, int width, int height) {

        Color[][] region = new Color[width][height];

        for(int xPos = 0; xPos < width; xPos++) {
            for(int yPos = 0; yPos < height; yPos++) {
                region[xPos][yPos] = source[x + xPos][y + yPos];
            }
        }

        return region;
    }

}
