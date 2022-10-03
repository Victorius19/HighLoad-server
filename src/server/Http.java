package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;

class Http implements Runnable {
    private final static char CR  = (char) 0x0D;
    private final static char LF  = (char) 0x0A;
    private final static String CRLF  = "" + CR + LF;
    protected Socket socket;
    private static String DIR_FILES;
    private static final int SIZE = 2048;
    private final int TIMEOUT = 15000;

    Http(Socket socket) {
        this.socket = socket;
        DIR_FILES = System.getProperty("user.dir");
    }

    private void selectCode(OutputStream output, String readLoop, String method) {
        switch (method) {
            case "GET": {
                String url = getRequestURL(readLoop);
                sendFile(url, output, false);
                break;
            }
            case "HEAD": {
                String url = getRequestURL(readLoop);
                sendFile(url, output, true);
                break;
            }
            default:
                header(output, 405, null, 0);
        }
    }

    private String readLoop(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder builder = new StringBuilder();
        String ln = null;

        while (true) {
            ln = reader.readLine();
            if (ln == null || ln.isEmpty()) {
                break;
            }
            builder.append(ln).append(System.getProperty("line.separator"));
        }

        return builder.toString();
    }

    private String getRequestURL(String header) {
        int from = header.indexOf(" ") + 1;
        if (from == 0)
            return DIR_FILES + "/index.html";

        int to = header.indexOf(" ", from);
        if (to == -1)
            return DIR_FILES + "/index.html";

        String url = header.substring(from, to);
        url = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8);
        if (url.lastIndexOf("/") == url.length() - 1)
            if (!url.contains("."))
                return DIR_FILES + url + "index.html";
            else url += "badPath";

        int paramIndex = url.indexOf("?");
        if (paramIndex != -1)
            url = url.substring(0, paramIndex);

        if (isURLDangerous(url))
            return null;

        return DIR_FILES + url;
    }

    private static String getRequestMethod(String header) {
        int to = header.indexOf(" ");
        
        if (to == -1) {
            return null;
        }

        return header.substring(0,to);
    }

    private void header(OutputStream out, int code, String mime, int size) {
        String header = createResponseHeader(code, mime, size);
        (new PrintStream(out, true, StandardCharsets.UTF_8)).print(header);
    }

    private void sendFile(String url, OutputStream out, Boolean isHead) {
        if (url == null) {
            header(out, 403, null, 0);
            return;
        }
        int code = 200;
        String mime = null;
        int size = 0;

        try {
            File file = new File(url);
            mime = getContentType(file);
            size = (int)file.length();
            FileInputStream fin = new FileInputStream(file);

            header(out, code, mime, size);

            if (!isHead) {
                int count;
                byte[] buffer = new byte[SIZE];
                while ((count = fin.read(buffer)) > 0) {
                    out.write(buffer, 0, count);
                }
            }
            fin.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            code = url.contains("/index.html") ? 403 : 404;
        }
        if (code != 200)
            header(out, code, mime, size);
    }

    private String createResponseHeader(int code, String contentType, int contentLength) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("HTTP/1.1 ").append(code).append(" ").append(getAnswer(code)).append(CRLF);
        buffer.append("Server: Java web-server" + CRLF);
        buffer.append("Connection: close" + CRLF);
        buffer.append("Date: ").append(new Date()).append(CRLF);
        buffer.append("Accept-Ranges: none " + CRLF);

        if (code == 200) {
            if (contentType != null)
                buffer.append("Content-Type: ").append(contentType).append(CRLF);
            if (contentLength != 0)
                buffer.append("Content-Length: ").append(contentLength).append(CRLF);
        }
        
        buffer.append(CRLF);
        return buffer.toString();
    }

    private String getContentType(File file) throws IOException {
        int index = file.getPath().lastIndexOf('.');
        if (index > 0) {
            if (file.getPath().substring(index + 1).equals("swf")) {
                return "application/x-shockwave-flash";
            }
        }

        return Files.probeContentType(file.toPath());
    }

    private String getAnswer(int code) {
        switch (code) {
            case 200:
                return "OK";
            case 405:
                return "Method not allowed";
            case 404:
                return "Not Found";
            case 403:
                return "Forbidden";
            default:
                return "Internal Server Error";
        }
    }

    private int subStrInStr(String origin, String subStr) {
        int count = 0;
        while (origin.contains(subStr)){
            origin = origin.replaceFirst(subStr, "");
            count++;
        }
        return count ;
    }

    private Boolean isURLDangerous(String url) {
        int backnesting = subStrInStr(url, "/..");
        if (backnesting > 0) {
            int nesting = subStrInStr(url, "/") - 2 * backnesting;
            return nesting < 0;
        }
        return false;
    }

    @Override
    public void run() {
        InputStream input = null;
        OutputStream output = null;

        try {
            socket.setSoTimeout(this.TIMEOUT);
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();

            String readLoop = readLoop(input);
            String method = getRequestMethod(readLoop);

            if (method == null) {
                throw new IOException();
            }

            selectCode(output, readLoop, method);
        } catch (IOException e) {
            
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                
            }
            try {
                if (output != null)
                    output.close();
                else
                    socket.close();
            } catch (IOException e) {
                
            }
        }
    }
}
