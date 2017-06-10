package ru.au.yaveyn.sd.messenger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyClient {
    private static final Logger logger = Logger.getLogger(MyClient.class.getName());

    private final ManagedChannel channel;
    private final MessengerGrpc.MessengerBlockingStub blockingStub;

    private String name;
    private String contactName;

    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public MyClient(String host, int port, String name) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true));
        this.name = name;
    }

    /**
     * Construct client for accessing RouteGuide server using the existing channel.
     */
    MyClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = MessengerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void talk() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String message = scanner.nextLine();
            Message request = Message.newBuilder()
                    .setMessage(message)
                    .setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setName(name)
                    .build();
            Message response;
            try {
                response = blockingStub.talk(request);
            } catch (StatusRuntimeException e) {
                logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
                return;
            }

            String newContactName = response.getName();
            if (contactName != null) {
                assert contactName.equals(newContactName);
            }
            contactName = newContactName;

//            logger.info("got message from " + contactName);
            System.out.println("[" + response.getTime() + "] " + contactName + ": " + response.getMessage());

            if (message.equals("EOC")) {
                break;
            }
        }
        logger.info("end of communication");
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        // default settings:
        String host = "localhost";
        int port = 50051;
        String name = "bronti-client";

        assert args.length <= 3;
        if (args.length >= 1) {
            name = args[0];
        }
        if (args.length >= 2) {
            host = args[1];
        }
        if (args.length >= 3) {
            port = Integer.parseInt(args[2]);
        }

        MyClient client = new MyClient(host, port, name);
        try {
            client.talk();
        } finally {
            client.shutdown();
        }
    }
}
