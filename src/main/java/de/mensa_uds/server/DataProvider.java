package de.mensa_uds.server;

import de.mensa_uds.data.Day;
import de.mensa_uds.data.Menu;
import de.mensa_uds.data.MenuStatus;
import de.mensa_uds.data.OpeningTimesStatus;
import de.mensa_uds.server.parser.XMLParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataProvider {

    private static final String URL_SB = "http://www.studentenwerk-saarland.de/_menu/actual/speiseplan-saarbruecken.xml";
    private static final String URL_HOM = "http://www.studentenwerk-saarland.de/_menu/actual/speiseplan-homburg.xml";

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private static DataProvider instance = null;
    private Timer timer;
    private TimerTask updateTask;

    private MenuStatus menuStatus;
    private Menu menuSB;
    private Menu menuHOM;
    private OpeningTimesStatus openingTimesStatus;
    private String openingTimesSB;
    private String openingTimesHOM;

    protected DataProvider() {
        updateTask = new TimerTask() {

            @Override
            public void run() {
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ss a zzz");

                System.out.println("[" + ft.format(now) + "] updateTask starting");
                update();
                System.out.println("[" + ft.format(now) + "] updateTask finished");
            }
        };

        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int unroundedMinutes = calendar.get(Calendar.MINUTE);
        int mod = unroundedMinutes % 15;
        calendar.add(Calendar.MINUTE, 15 - mod);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat ft = new SimpleDateFormat("E dd.MM.yyyy 'at' hh:mm:ss a zzz");
        System.out.println("scheduling next regular update for " + ft.format(calendar.getTime()));

        timer = new Timer();
        timer.scheduleAtFixedRate(updateTask, calendar.getTime(), 15 * 60 * 1000);
    }

    public static DataProvider getInstance() {
        if (instance == null) {
            instance = new DataProvider();
        }
        return instance;
    }

    public MenuStatus getMenuStatus() {
        try {
            lock.readLock().lock();
            return menuStatus;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Menu getMenuSB() {
        try {
            lock.readLock().lock();
            return menuSB;
        } finally {
            lock.readLock().unlock();
        }

    }

    public Menu getMenuHOM() {
        try {
            lock.readLock().lock();
            return menuHOM;
        } finally {
            lock.readLock().unlock();
        }
    }

    public OpeningTimesStatus getOpeningTimesStatus() {
        try {
            lock.readLock().lock();
            return openingTimesStatus;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getOpeningTimesSB() {
        try {
            lock.readLock().lock();
            return openingTimesSB;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getOpeningTimesHOM() {
        try {
            lock.readLock().lock();
            return openingTimesHOM;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());

            OpeningTimesStatus openingTimesStatus = new OpeningTimesStatus();
            openingTimesStatus.setTimestamp(timestamp);

            String openingTimesSBTimestamp = getFileTimestamp("opening_sb.html");
            String openingTimesHOMTimestamp = getFileTimestamp("opening_hom.html");

            openingTimesStatus.setTimestampSB(openingTimesSBTimestamp);
            openingTimesStatus.setTimestampHOM(openingTimesHOMTimestamp);

            String openingTimesSB = readFile("opening_sb.html");
            String openingTimesHOM = readFile("opening_hom.html");

            MenuStatus menuStatus = new MenuStatus();
            menuStatus.setTimestamp(timestamp);

            System.out.println("opening connection");
            URLConnection conn_sb = new URL(URL_SB).openConnection();
            URLConnection conn_hom = new URL(URL_HOM).openConnection();

            long timestampSB = conn_sb.getLastModified();
            long timestampHOM = conn_hom.getLastModified();

            if (this.menuStatus != null) {
                boolean uptodateSB = timestampSB == Long.parseLong(this.menuStatus.getFileTimestampSB());
                boolean uptodateHOM = timestampHOM == Long.parseLong(this.menuStatus.getFileTimestampHOM());

                if (uptodateSB && uptodateHOM) {
                    System.out.println("data is up-to-date, no need for updating");
                    return;
                }
            }

            System.out.println("server data is newer, updating");

            menuStatus.setFileTimestampSB(String.valueOf(timestampSB));
            menuStatus.setFileTimestampHOM(String.valueOf(timestampHOM));

            InputStream is_sb = conn_sb.getInputStream();
            InputStream is_hom = conn_hom.getInputStream();

            System.out.println("parsing files");
            Day[] days_sb = XMLParser.parseFile(is_sb);
            Day[] days_hom = XMLParser.parseFile(is_hom);
            System.out.println("done parsing");

            Menu menuSB = new Menu();
            menuSB.setTimestamp(timestamp);
            menuSB.setDays(days_sb);
            Menu menuHOM = new Menu();
            menuHOM.setTimestamp(timestamp);
            menuHOM.setDays(days_hom);

            try {
                lock.writeLock().lock();
                this.menuSB = menuSB;
                this.menuHOM = menuHOM;
                this.menuStatus = menuStatus;

                this.openingTimesStatus = openingTimesStatus;
                this.openingTimesSB = openingTimesSB;
                this.openingTimesHOM = openingTimesHOM;
            } finally {
                lock.writeLock().unlock();
            }

        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    private String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private String getFileTimestamp(String path) {
        File file = new File(path);
        return String.valueOf(file.lastModified());
    }

}
