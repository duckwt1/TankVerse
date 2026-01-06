package com.tank2d.tankverse.effect;

import com.tank2d.tankverse.utils.Constant;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;
import java.util.List;

public class ImageExplosionEffect extends Effect {

    private final List<ImageFragment> fragments = new ArrayList<>();

    public ImageExplosionEffect(
            double x,
            double y,
            Image tankImage,
            int pieces,
            double radius
    ) {
        int cols = (int) Math.sqrt(pieces);
        int rows = cols;

        double pieceW = tankImage.getWidth() / cols;
        double pieceH = tankImage.getHeight() / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                WritableImage piece = new WritableImage(
                        tankImage.getPixelReader(),
                        (int) (c * pieceW),
                        (int) (r * pieceH),
                        (int) pieceW,
                        (int) pieceH
                );

                double px = x + c * pieceW - tankImage.getWidth() / 2;
                double py = y + r * pieceH - tankImage.getHeight() / 2;

                fragments.add(
                        new ImageFragment(px, py, piece, radius)
                );
            }
        }
    }

    @Override
    public void update(double dt) {
        fragments.removeIf(f -> {
            f.update(dt);
            return f.isDead();
        });

        if (fragments.isEmpty()) finished = true;
    }

    @Override
    public void draw(GraphicsContext gc, double camX, double camY) {

        for (ImageFragment f : fragments) {

            double sx =
                    Constant.SCREEN_WIDTH / 2 + (f.x - camX);
            double sy =
                    Constant.SCREEN_HEIGHT / 2 + (f.y - camY);

            gc.save();
            gc.translate(sx, sy);
            gc.rotate(f.rotation);
            gc.drawImage(
                    f.image,
                    -f.image.getWidth() / 2,
                    -f.image.getHeight() / 2
            );
            gc.restore();
        }
    }
}
