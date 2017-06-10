package ru.au.yaveyn.sd.messenger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * ServerSide that manages startup/shutdown of a {@code Greeter} server.
 */
public class MyServer {
    private static final Logger logger = Logger.getLogger(MyServer.class.getName());

    private Server server;
    private String name;
    private int port;

    public MyServer(String name, int port) {
        this.name = name;
        this.port = port;
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl(name))
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                MyServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // default settings:
        int port = 50051;
        String name = "bronti-server";

        assert args.length <= 2;
        if (args.length >= 1) {
            name = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }

        final MyServer server = new MyServer(name, port);
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends MessengerGrpc.MessengerImplBase {

        private String contactName;
        private String name;

        public GreeterImpl(String name) {
            this.name = name;
        }

        @Override
        public void talk(Message request, StreamObserver<Message> responseObserver) {
            String newContactName = request.getName();
            if (contactName != null) {
                assert contactName.equals(newContactName);
            }
            contactName = newContactName;
//            logger.info("got message from " + contactName);

            System.out.println("[" + request.getTime() + "] " + contactName + ": " + request.getMessage());
            Scanner scanner = new Scanner(System.in);
            Message reply = Message.newBuilder()
                    .setMessage(scanner.nextLine())
                    .setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setName(name)
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
