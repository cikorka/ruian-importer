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

package cz.ruian.importer;

import com.fordfrog.ruian2pgsql.Config;
import cz.ruian.importer.config.RuianConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Paths;

@EnableScheduling
@SpringBootApplication
public class RuianImporterApplication implements InitializingBean {

    private final RuianConfig config;

    public RuianImporterApplication(RuianConfig config) {
        this.config = config;
    }


    public static void main(String[] args) {
        SpringApplication.run(RuianImporterApplication.class, args);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Config.setConvertToEWKT(config.isConvertToEwkt());
        Config.setLinearizeEWKT(config.isLinearizeEwkt());
        Config.setCreateTables(config.isCreateTables());
        Config.setDbConnectionUrl(config.getDbConnectionUrl());
        Config.setDestinationSrid(config.getDestSrid());
        Config.setDebug(config.isDebug());
        Config.setDryRun(config.isDryRun());
        Config.setIgnoreInvalidGML(config.isIgnoreInvalidGml());
        if (config.getLogFile() != null) {
            Config.setLogFilePath(Paths.get(config.getLogFile()));

        }
        Config.setNoGis(config.isNoGis());
        Config.setResetTransactionIds(config.isResetTransactionIds());
        Config.setTruncateAll(config.isTruncateAll());

    }
}
