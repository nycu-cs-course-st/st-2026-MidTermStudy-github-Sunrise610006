import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class LocalMockServer {
    // fixed key for this implementation
    private static final byte[] TEST_KEY = "local-study-key-only".getBytes(StandardCharsets.UTF_8);

    private static String generateSignature(String file)
            throws GeneralSecurityException {

        // create MAC object that use SHA256 generate HMAC
        Mac mac = Mac.getInstance("HmacSHA256");

        // initialize HMAC algo by TEST_KEY
        SecretKeySpec key = new SecretKeySpec(
                TEST_KEY, "HmacSHA256");

        mac.init(key);

        // compute HMAC
        byte[] result = mac.doFinal(
                file.getBytes(StandardCharsets.UTF_8));

        // transform to hex (HMAC-SHA256 output 32 bytes -> 64 hex)
        StringBuilder hex = new StringBuilder();

        for (byte b : result) {
            hex.append(String.format("%02x", b & 0xff));
        }

        return hex.toString();
    }

    public static void main(String[] args) throws Exception {

        // version mode option
        final boolean faultyMode = args.length > 0 && args[0].equals("--faulty");

        System.out.println(
                "MODE: " + (faultyMode ? "FAULTY" : "CORRECT"));

        // create http server for local connecting only
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 8000), 0);

        // set response of download request
        server.createContext("/download", exchange -> {

            // get Query String after '?' from URL
            String query = exchange.getRequestURI().getRawQuery();

            String file = null;
            String sig = null;

            if (query != null) {
                String[] parameters = query.split("&");

                // ex: parts[0] = file, parts[1] = A
                for (String parameter : parameters) {
                    String[] parts = parameter.split("=", 2);

                    if (parts.length != 2) {
                        continue;
                    }

                    String value = java.net.URLDecoder.decode(parts[1], "UTF-8");

                    if (parts[0].equals("file")) {
                        file = value;
                    } else if (parts[0].equals("sig")) {
                        sig = value;
                    }
                }
            }

            // determine responds and http status according to the file
            String response;
            int status;

            if (file == null) {
                response = "Missing file parameter";
                status = 400;

            } else if (!file.equals("A") && !file.equals("B")) {
                response = "File Not Found";
                status = 404;

            } else if (sig == null) {
                response = "Access Denied";
                status = 403;

            } else {
                try {
                    String expectedSig = generateSignature(file);

                    // check if two signatures are equal
                    boolean valid = MessageDigest.isEqual(
                            expectedSig.getBytes(StandardCharsets.US_ASCII),
                            sig.getBytes(StandardCharsets.US_ASCII));

                    // orginal: check sig_provide = HMAC_K(request_file)
                    // faultymode: check sig_provide = sig_A / sig_B
                    // attacker could get file A/B with sig_B or sig_A
                    if (faultyMode) {

                        boolean matchA = MessageDigest.isEqual(
                                generateSignature("A").getBytes(StandardCharsets.US_ASCII),
                                sig.getBytes(StandardCharsets.US_ASCII));

                        boolean matchB = MessageDigest.isEqual(
                                generateSignature("B").getBytes(StandardCharsets.US_ASCII),
                                sig.getBytes(StandardCharsets.US_ASCII));

                        valid = matchA || matchB;
                    }

                    if (valid) {
                        response = file.equals("A") ? "Content A" : "Content B";
                        status = 200;
                    } else {
                        response = "Access Denied";
                        status = 403;
                    }

                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    response = "Internal Server Error";
                    status = 500;
                }
            }

            if (status == 403) {
                response = "<html>"
                        + "<head><title>Access Denied</title></head>"
                        + "<body><h1>Access Denied</h1></body>"
                        + "</html>";
            }

            // send HTTP Response
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    status == 403
                            ? "text/html; charset=UTF-8"
                            : "text/plain; charset=UTF-8");

            exchange.sendResponseHeaders(status, bytes.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        server.start();
        System.out.println("Server started on port 8000");

        String sigA = generateSignature("A");
        String sigB = generateSignature("B");

        System.out.println("Signature A: " + sigA);
        System.out.println("Signature B: " + sigB);

        System.out.println(
                "URL A: http://127.0.0.1:8000/download?file=A&sig=" + sigA);

        System.out.println(
                "URL B: http://127.0.0.1:8000/download?file=B&sig=" + sigB);

    }
}
