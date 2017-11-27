package krypto.elgamal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {

    public static void main(String[] args) throws IOException {

        if (args.length > 0 && args[0].equals("encr")) {
            String msgPath = args[1];
            int bits = Integer.parseInt(args[2]);
            int conf = Integer.parseInt(args[3]);
            int radix = Integer.parseInt(args[4]);
            String pubKeyPath = args[5];
            String privKeyPath = args[6];
            String cipherPath = args[7];

            Elgamal.PrivateKey privateKey = new Elgamal.PrivateKey();
            Elgamal.PublicKey publicKey = new Elgamal.PublicKey();
            Elgamal.generateKeys(bits, conf, privateKey, publicKey);
            String cipher = Elgamal.encrypt(readMsg(msgPath), radix, publicKey);

            saveToFile(pubKeyToArr(publicKey, radix), pubKeyPath);
            saveToFile(privKeyToArr(privateKey, radix), privKeyPath);
            saveMsgToFile(cipher, cipherPath);

            System.out.println("Encrypted.");
            return;
        }

        if (args.length > 0 && args[0].equals("decr")) {
            String cipherPath = args[1];
            String privKeyPath = args[2];
            int radix = Integer.parseInt(args[3]);
            String decmsgPath = args[4];

            Elgamal.PrivateKey privateKey = privKeyFromArr(readFile(privKeyPath), radix);
            String msg = Elgamal.decrypt(readMsg(cipherPath), radix, privateKey);

            saveMsgToFile(msg, decmsgPath);

            System.out.println("Decrypted.");
            return;
        }

        String usage = "Usage:\n" +
                "encr {path to msg} {prime bits num} {prime test confidence} {radix} " +
                "{path to save pub key} {path to save priv key} {path to save cipher}\n" +
                "decr {path to cipher} {priv key path} {radix} {path to save decrypted message}";

        System.out.println(usage);
    }

    public static void saveToFile(String[] data, String fileName) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
        for (String i : data) printWriter.println(i);
        printWriter.close();
    }

    public static void saveMsgToFile(String msg, String fileName) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName));
        printWriter.print(msg);
        printWriter.close();
    }

    public static List<String> readFile(String fileName) throws IOException {
        return Files.readAllLines(Paths.get(fileName));
    }

    public static String readMsg(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    public static String[] pubKeyToArr(Elgamal.PublicKey key, int radix) {
        return new String[]{
                key.p.toString(radix),
                key.g.toString(radix),
                key.h.toString(radix),
                String.valueOf(key.bits)};
    }

    public static String[] privKeyToArr(Elgamal.PrivateKey key, int radix) {
        return new String[]{
                key.p.toString(radix),
                key.g.toString(radix),
                key.x.toString(radix),
                String.valueOf(key.bits)
        };
    }

    public static Elgamal.PrivateKey privKeyFromArr(List<String> data, int radix) {
        Elgamal.PrivateKey key = new Elgamal.PrivateKey();
        key.p = new BigInteger(data.get(0), radix);
        key.g = new BigInteger(data.get(1), radix);
        key.x = new BigInteger(data.get(2), radix);
        key.bits = Integer.parseInt(data.get(3));
        return key;
    }
}
