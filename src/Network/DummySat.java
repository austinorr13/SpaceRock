package Network;

import Commands.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

/**
 */
public class DummySat extends Thread {

    public static final int SAT_PORT = 32000;
    public static final long DEFAULT_SEED = 109;

    private static final double ADD_ASTEROID_CHANCE = .2f;
    private static final double VIEW_X = 1000f;
    private static final double VIEW_Y = 1000f;
    private static final double MEAN_ASTEROID_SIZE = 20f;
    private static final double ASTEROID_SIZE_STDDEV = 10f;
    private static final double MAX_ASTEROID_SPEED = 3f;
    private static final int MAX_ASTEROIDS = 10;
    private final List<DummyAsteroid> asteroids = new ArrayList<>();
    private Rectangle2D viewRect = new Rectangle2D.Double(0, 0, VIEW_X, VIEW_Y);
    private long currentID = 0;
    private Random rand;

    private int chunkWidth = 50;
    private int chunkHeight = 50;
    private boolean cameraIsOn = true;
    private double cameraZoom = 1f;


    public DummySat(long seed) {
        rand = new Random(seed);
    }


    public DummySat() {
        this(DEFAULT_SEED);
        this.setDaemon(true);
    }


    /**
     Start the DummySat.  Don't directly use this method (as in Thread.run()).  Prefer
     DummySat.start().  This has the effect of created an additional daemon thread that manages
     the collection of DummyAsteroids as soon as a socket connection is accepted.
     */
    @Override
    public void run() {


        try {
            /* only accepting one connection at a time for now */
            Socket sock = new ServerSocket(SAT_PORT).accept();

            /* Order of stream creation is important here. ObjectInputStream creation blocks
            until a header is written by the opposing OutputStream */
            SecureOutputStream out = new SecureOutputStream(sock.getOutputStream());
            SecureInputStream in = new SecureInputStream(sock.getInputStream());

            /* Tick asteroids once every 1000ms */
            startDummyAsteroids(out, 1000);

            while (true) {
                Object o = in.readObject();
                logReceived(o);
                if (o instanceof OutgoingCameraSpec) {
                    /* Set cam spec */
                } else if (o instanceof ImageRequest) {
                    ImageRequest req = (ImageRequest) o;
                    out.writeObject(makeDummyIncomingImage(req.id));
                }
            }

        } catch (EOFException e) {
            /* Socket closed,  */

        } catch (IOException e) { /* Legitimate problems */
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }


    /*
    Create an image given an asteroid asteroidID.  Currently just writes the asteroidID as a string to an image
     */
    private ImageData makeDummyIncomingImage(long id) {
        String idStr = String.format("%d", id);
        int padding = 5;

        AsteroidData rock = getAsteroidByID(id).orElse(DummyAsteroid.BAD_ASTEROID);

        BufferedImage img = new BufferedImage(chunkWidth, chunkHeight,
                                              BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = img.getGraphics();

        g.setColor(Color.white);
        g.fillRect(0, 0, chunkWidth, chunkHeight);

        g.setColor(Color.black);
        g.drawString(idStr, padding, chunkHeight - padding);

        int xOffset = (int) (rock.getLoc().getX() / (rock.getSize() / 2));
        int yOffset = (int) (rock.getLoc().getY() / (rock.getSize() / 2));

        return new ImageData(img, id, xOffset, yOffset);
    }


    private Optional<DummyAsteroid> getAsteroidByID(long id) {
        return asteroids.stream().filter(a -> a.getID() == id).findAny();
    }


    /*
    Log an Object received on the socket input stream.  Simply prints to stdout for now.
     */
    private void logReceived(Object o) {
        System.out.println("Sat received " + o.toString());
    }


    /**
     Start a Timer that steps the Asteroids in the background, send new data out on the socket
     output stream with each Timer tick.

     @param out ObjectOutputStream to write asteroid data to with each tick
     @param period Milliseconds between each Timer tick
     */
    private void startDummyAsteroids(SecureOutputStream out, long period) {

        /* Initialize the sat with a bunch of asteroids */
        IntStream.range(0, MAX_ASTEROIDS).forEach(i -> asteroids.add(randAsteroid()));

        new Timer("Asteroid Iteration", true)
                .scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        iterateAsteroids();
                        try {
                            out.writeObject(makeFrameData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, period);
    }


    /**
     @return a new FrameData object for transmission to the operator station.
     */
    private FrameData makeFrameData() {
        AsteroidData[] data = makeAsteroidArray();
        long time = System.currentTimeMillis();
        return new AsteroidFrame(data, time);
    }


    /* return an AsteroidData[] array created from the DummySat's asteroids list */
    private AsteroidData[] makeAsteroidArray() {
        int len = asteroids.size();
        AsteroidData[] data = new AsteroidData[len];
        for (int i = 0; i < len; i++) {
            /* It's important that we *copy* here.  Java's Object streams don't seem to honor
            mutable objects.  If you send the same object (as in, the same memory address), the
            underlying data seems to be ignored. I'm not sure what side this occurs on, but it's
            something we have to take into account. */
            data[i] = asteroids.get(i).copy();
        }
        return data;
    }


    /**
     Step all the DummyAsteroids currently being managed by this DummySat.
     */
    private void iterateAsteroids() {

        asteroids.forEach(DummyAsteroid::step);
        asteroids.removeIf(a -> isInView(a));

        if (rand.nextDouble() < ADD_ASTEROID_CHANCE && asteroids.size() < MAX_ASTEROIDS) {
            asteroids.add(randAsteroid());
        }
    }


    /**
     @param  asteroid  AsteroidData to test
     @return true if the given asteroid is in the camera's view.
     */
    private boolean isInView(AsteroidData asteroid) {
        return viewRect.contains(asteroid.getLoc());
    }


    /** @return random DummyAsteroid parametrized by VIEW_X, VIEW_Y, and MEAN_ASTEROID_SIZE */
    private DummyAsteroid randAsteroid() {
        return new DummyAsteroid(randPoint(), randVeloc(), randSize(), currentID++);
    }


    /** @return a random DummyAsteroid velocity */
    private Point2D randVeloc() {
        double x = rand.nextDouble() * 2 - 1;
        double y = Math.sqrt(1 - Math.pow(x, 2)) * (rand.nextBoolean() ? 1 : -1);
        double speed = MAX_ASTEROID_SPEED * rand.nextDouble();
        x *= speed;
        y *= speed;

        return new Point2D.Double(x, y);
    }


    /** @return a random Asteroid size */
    private double randSize() {
        return Math.abs(rand.nextGaussian() * ASTEROID_SIZE_STDDEV + MEAN_ASTEROID_SIZE);
    }


    /** @return a random Point2D within the DummySat's viewbox */
    private Point2D randPoint() {
        double x = rand.nextDouble() * VIEW_X;
        double y = rand.nextDouble() * VIEW_Y;
        return new Point2D.Double(x, y);
    }
}
