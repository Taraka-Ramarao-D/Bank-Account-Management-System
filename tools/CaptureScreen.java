import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Temporary screenshot helper: captures the whole Xvfb screen with
 * java.awt.Robot, which is the genuine display of the running application.
 * Usage: java CaptureScreen <output-png>
 */
public class CaptureScreen {
    public static void main(String[] args) throws Exception {
        Thread.sleep(500);
        BufferedImage image = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ImageIO.write(image, "png", new File(args[0]));
        System.out.println("Saved " + args[0] + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }
}
