/*
 * Copyright 2017 Petr Jerabek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cz.ruian.importer.services;

import com.fordfrog.ruian2pgsql.Config;
import com.fordfrog.ruian2pgsql.convertors.MainConvertor;
import com.fordfrog.ruian2pgsql.utils.Log;
import cz.ruian.importer.logging.LogOutputStream;
import cz.ruian.importer.utils.VdpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class ImportServiceImpl implements ImportService {

    private final static Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
    private static final String SQL_EXIST = "SELECT 1 FROM hlavicka WHERE hlavicka.metadata LIKE ?;";
    private static final String SQL_LAST_IMPORT = "SELECT datum FROM hlavicka WHERE typ_davky LIKE 'Prirustek' ORDER BY import_timestamp DESC LIMIT 1;";

    private final VdpUtils vdpUtils;

    @Autowired
    public ImportServiceImpl(VdpUtils vdpUtils) {
        this.vdpUtils = vdpUtils;
    }

    @Override
    @Scheduled(cron = "${cron}")
    //@Scheduled(cron = "0 0 4 * * ?")
    public void process() throws Exception {

        final Date date = getLastDeltaFileDate();

        if (date == null || date.getTime() <= (System.currentTimeMillis() - (Duration.ofDays(30).getSeconds() * 1000))) {

            //
            // Full Import
            //

            logger.info("Run full import...");

            /*
            final List<URL> urlList = new ArrayList<>();
            for (final URL url : vdpUtils.getLinksForProcessing()) {
                final String filename = FilenameUtils.getName(url.getPath());
                if (!isImported(filename)) {
                    urlList.add(url);
                } else {
                    logger.info(filename + " is already imported.");
                }
            }
             */
            final List<URL> urlList = vdpUtils.getLinksForProcessing();
            if (urlList.size() > 0) {
                doimport(urlList);
            }

            //
            // Compute from and Process delta files
            //

            final Calendar from = Calendar.getInstance();
            from.add(Calendar.MONTH, -1); // add -1 month to current month
            from.set(Calendar.DATE, 1); // set DATE to 1, so first date of previous month
            from.set(Calendar.DATE, from.getActualMaximum(Calendar.DAY_OF_MONTH)); // set actual maximum date of previous month

            logger.info("Run delta import from " + from.toInstant() + "...");

            final List<URL> deltaUrlList = vdpUtils.getLinksForProcessing(from);
            if (deltaUrlList.size() > 0) {
                doimport(deltaUrlList); // process delta files
            }

            logger.info("Full import finished...");

        } else {

            //
            // Delta Import
            //

            final Calendar from = Calendar.getInstance();
            from.setTime(date);

            logger.info("Run delta import from " + from.toInstant() + "...");

            final List<URL> urlList = new ArrayList<>();

            final List<URL> deltaUrlList = vdpUtils.getLinksForProcessing(from);

            for (final URL url : deltaUrlList) {
                final String filename = FilenameUtils.getName(url.getPath());
                if (!isImported(filename)) {
                    urlList.add(url);
                }
            }

            if (urlList.size() > 0) {
                doimport(urlList);
            }

            logger.info("Delta import from " + from.toInstant() + " finished...");
        }
    }

    private void doimport(final List<URL> urlList) throws Exception {

        //
        // Create temp dir & download files
        //

        final Path temp = Files.createTempDirectory(Paths.get("/var/tmp"), "ruian");
        logger.info("Create temp directory " + temp);
        FileUtils.forceDeleteOnExit(temp.toFile());

        Config.setInputDirPath(temp);

        vdpUtils.download(urlList, temp.toFile());

        //
        // Run RUIAN importer
        //

        try (final Writer logFile = new OutputStreamWriter(Config.getLogFilePath() == null ? new LogOutputStream(logger) : Files.newOutputStream(Config.getLogFilePath()), "UTF-8")) {
            Log.setLogWriter(logFile);
            MainConvertor.convert();
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to create log writer", ex);
        } finally {
            try {
                FileUtils.forceDelete(temp.toFile());
            } catch (IOException e) {
                logger.error("Error delete temp directory", e);
            }
        }

    }

    private boolean isImported(String filename) throws SQLException {
        try (final Connection connection = DriverManager.getConnection(Config.getDbConnectionUrl())) {
            final PreparedStatement statement = connection.prepareStatement(SQL_EXIST);
            statement.setString(1, "%" + filename.replace(".xml.zip", ".xml") + "%");
            final ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }

    private Date getLastDeltaFileDate() {
        try {
            try (final Connection connection = DriverManager.getConnection(Config.getDbConnectionUrl())) {
                final PreparedStatement statement = connection.prepareStatement(SQL_LAST_IMPORT);
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getDate("DATUM");
                }
            }
        } catch (SQLException e) {
            logger.error("Error when get last delta file date.");
        }
        return null;
    }

}
