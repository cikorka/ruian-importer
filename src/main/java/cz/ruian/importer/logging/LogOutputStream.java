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

package cz.ruian.importer.logging;

import org.slf4j.Logger;

import java.io.OutputStream;

public class LogOutputStream extends OutputStream {

    private Logger logger;

    private String mem;
    private String lineSeparator;

    public LogOutputStream(Logger logger) {
        this.mem = "";
        this.logger = logger;
        lineSeparator = System.getProperty("line.separator");
    }

    public void write(int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        mem = mem + new String(bytes);

        if (mem.endsWith(lineSeparator)) {
            mem = mem.substring(0, mem.length() - 1);
            flush();
        }
    }

    public void flush() {
        if (mem.length() == 0 || mem.equals(lineSeparator)) {
            // avoid empty records
            return;
        }

        mem = mem.trim();

        if (mem.toLowerCase().contains("failed")) {
            logger.error(mem);
        } else if (mem.toLowerCase().contains("warning")) {
            logger.warn(mem);
        } else {
            logger.info(mem);
        }
        mem = "";
    }

}
