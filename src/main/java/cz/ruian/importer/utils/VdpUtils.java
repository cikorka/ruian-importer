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

package cz.ruian.importer.utils;

import cz.ruian.importer.config.RuianConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class VdpUtils {

    private static final Logger logger = LoggerFactory.getLogger(VdpUtils.class);

    private final RuianConfig ruianConfig;

    @Autowired
    public VdpUtils(RuianConfig ruianConfig) {
        this.ruianConfig = ruianConfig;
    }

    private String[] getLinkListUrls() {
        if (ruianConfig.isNoGis()) {
            return new String[]{
                    "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky&crKopie=true&page&casovyRozsah=U&datum&upStatAzZsj=true&upObecAPodrazene=false&uzemniPrvky=ST&dsZakladni=true&dsKompletni=false&datovaSada=Z&vyZakladni=true&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=false&vyVlajkyAZnaky=false&vyber=vyZakladni&kodVusc&kodOrp&kodOb&mediaType=text",
                    "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky&crKopie=true&page&casovyRozsah=U&datum&upStatAzZsj=false&upObecAPodrazene=true&uzemniPrvky=OB&dsZakladni=true&dsKompletni=false&datovaSada=Z&vyZakladni=true&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=false&vyVlajkyAZnaky=false&vyber=vyZakladni&kodVusc&kodOrp&kodOb&mediaType=text"
            };

        } else {
            return new String[]{
                    "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky&crKopie=true&page&casovyRozsah=U&datum&upStatAzZsj=true&upObecAPodrazene=false&uzemniPrvky=ST&dsZakladni=false&dsKompletni=true&datovaSada=K&vyZakladni=false&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=true&vyVlajkyAZnaky=false&vyber=vyZakladniAOrigHranice&kodVusc&kodOrp&kodOb&mediaType=text",
                    "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky&crKopie=true&page&casovyRozsah=U&datum&upStatAzZsj=false&upObecAPodrazene=true&uzemniPrvky=OB&dsZakladni=false&dsKompletni=true&datovaSada=K&vyZakladni=false&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=true&vyVlajkyAZnaky=false&vyber=vyZakladniAOrigHranice&kodVusc&kodOrp&kodOb&mediaType=text"
            };
        }
    }

    private String getDeltaLinkListUrls() {
        if (ruianConfig.isNoGis()) {
            return "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky=true&crKopie&page&casovyRozsah=Z&datum=%d-%02d-%02d&upStatAzZsj=true&upObecAPodrazene=false&uzemniPrvky=ST&dsZakladni=true&dsKompletni=false&datovaSada=Z&vyZakladni=true&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=false&vyVlajkyAZnaky=false&vyber=vyZakladni&kodVusc&kodOrp&kodOb&mediaType=text";

        } else {
            return "https://vdp.cuzk.cz/vdp/ruian/vymennyformat?crPrirustky=true&crKopie&page&casovyRozsah=Z&datum=%d-%02d-%02d&upStatAzZsj=true&upObecAPodrazene=false&uzemniPrvky=ST&dsZakladni=false&dsKompletni=true&datovaSada=K&vyZakladni=false&vyZakladniAGenHranice=false&vyZakladniAOrigHranice=true&vyVlajkyAZnaky=false&vyber=vyZakladniAOrigHranice&kodVusc&kodOrp&kodOb&mediaType=text";
        }
    }

    /**
     * Get Links for processing
     *
     * @return List of URLs for download
     * @throws IOException
     */
    public List<URL> getLinksForProcessing() throws IOException {
        logger.info("Actual file prefix is " + getActualFilePrefix());
        final List<URL> links = new ArrayList<>();
        for (final String linkList : getLinkListUrls()) {
            for (final URL link : getLinks(linkList)) {
                for (final String prefix : getActualFilePrefix()) {
                    //if (link.toString().contains(prefix)) {
                    links.add(link);
                    //}
                }
            }
        }
        Collections.reverse(links);
        return links;
    }

    /**
     * Get Delta Links for processing
     *
     * @param from Calendar object
     * @return List of delta URLs for download
     * @throws IOException
     */
    @SuppressWarnings("WeakerAccess")
    public List<URL> getLinksForProcessing(final Calendar from) throws IOException {
        final String link = String.format(getDeltaLinkListUrls(), from.get(Calendar.DAY_OF_MONTH), from.get(Calendar.MONTH) + 1, from.get(Calendar.YEAR));
        final List<URL> links = new ArrayList<>();
        links.addAll(getLinks(link));
        return links;
    }

    @SuppressWarnings("WeakerAccess")
    public void download(final List<URL> urls, File destination) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (final URL url : urls) {
            executorService.submit(() -> {
                doDownload(destination, url);
            });
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(100L);
        }
    }

    private void doDownload(File destination, URL url) {
        File file = new File(destination, FilenameUtils.getName(url.getPath()));
        try {
            logger.info(String.format("Downloading %s to file %s", url, file));
            FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
            logger.error("Error when downloading file.", e);
            doDownload(destination, url);
        }
    }

    private List<URL> getLinks(final String urlToRead) throws IOException {
        final List<URL> result = new ArrayList<>();
        final URL url = new URL(urlToRead);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(new URL(line.trim()));
        }
        reader.close();
        return result;
    }

    private List<String> getActualFilePrefix() {
        List<String> prefixes = new ArrayList<>();
        final Calendar aCalendar = Calendar.getInstance();
        aCalendar.set(Calendar.DATE, 0);
        prefixes.add(
                String.format("%d%02d%02d", aCalendar.get(Calendar.YEAR), aCalendar.get(Calendar.MONTH) + 1, aCalendar.get(Calendar.DAY_OF_MONTH))
        );
        aCalendar.set(Calendar.DATE, 1); // set DATE to 1, so first date of previous month
        prefixes.add(
                String.format("%d%02d%02d", aCalendar.get(Calendar.YEAR), aCalendar.get(Calendar.MONTH) + 2, aCalendar.get(Calendar.DAY_OF_MONTH))
        );
        return prefixes;
    }

}
